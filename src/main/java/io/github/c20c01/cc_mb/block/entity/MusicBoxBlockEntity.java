package io.github.c20c01.cc_mb.block.entity;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.block.MusicBoxBlock;
import io.github.c20c01.cc_mb.client.NoteGridDataManager;
import io.github.c20c01.cc_mb.data.NoteGridData;
import io.github.c20c01.cc_mb.util.BlockUtils;
import io.github.c20c01.cc_mb.util.NoteGridUtils;
import io.github.c20c01.cc_mb.util.player.MusicBoxPlayer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class MusicBoxBlockEntity extends AbstractItemLoaderBlockEntity implements MusicBoxPlayer.Listener {
    public static final String NOTE_GRID = "note_grid";
    private final MusicBoxPlayer PLAYER;
    private boolean playNextBeat = false; // whether ask the client to play next beat

    public MusicBoxBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(CCMain.MUSIC_BOX_BLOCK_ENTITY, blockPos, blockState, NOTE_GRID);
        PLAYER = new MusicBoxPlayer(this);
    }

    public static void tick(Level level, BlockPos blockPos, BlockState blockState, MusicBoxBlockEntity blockEntity) {
        blockEntity.PLAYER.update(level, blockPos, blockState);
        blockEntity.PLAYER.tick();
    }

    @Override
    public void load(CompoundTag tag) {
        if (tag.contains("update")) {
            handleUpdateTag(tag);
        } else {
            super.load(tag);
            PLAYER.load(tag);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        PLAYER.saveAdditional(tag);
    }

    public void handleUpdateTag(CompoundTag tag) {
        if (tag.contains("note_grid_hash")) {
            // has note grid, load data and play next beat if needed
            NoteGridDataManager.getInstance().getNoteGridData(tag.getInt("note_grid_hash"), getBlockPos(), PLAYER::setData);
            if (tag.getBoolean("play_next_beat")) {
                PLAYER.nextBeat(level, getBlockPos(), getBlockState());
            }
        } else {
            // no note grid, remove data
            NoteGridData data = PLAYER.getData();
            if (data != null) {
                NoteGridDataManager.getInstance().markRemovable(data.hashCode());
                PLAYER.setData(null);
            }
        }
        // update the player's state
        PLAYER.loadUpdateTag(tag);
    }

    public Optional<NoteGridData> getPlayerData() {
        return Optional.ofNullable(PLAYER.getData());
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        tag.putByte("update", (byte) 0);
        PLAYER.saveUpdateTag(tag);
        NoteGridData data = PLAYER.getData();
        if (data != null) {
            tag.putInt("note_grid_hash", data.hashCode());
            tag.putBoolean("play_next_beat", playNextBeat);
        }
        playNextBeat = false;
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void setRemoved() {
        super.setRemoved();
        if (PLAYER.getData() != null) {
            NoteGridDataManager.getInstance().markRemovable(PLAYER.getData().hashCode());
        }
    }

    @Override
    protected void loadItem(ItemStack noteGrid) {
        PLAYER.setData(NoteGridData.ofNoteGrid(noteGrid));
        if (level != null) {
            BlockUtils.changeProperty(level, getBlockPos(), getBlockState(), MusicBoxBlock.HAS_NOTE_GRID, true);
        }
    }

    @Override
    protected void unloadItem() {
        PLAYER.reset();
        if (level != null) {
            BlockUtils.changeProperty(level, getBlockPos(), getBlockState(), MusicBoxBlock.HAS_NOTE_GRID, false);
        }
    }

    @Override
    public boolean canPlaceItem(ItemStack itemStack) {
        return itemStack.is(CCMain.NOTE_GRID_ITEM);
    }

    @Override
    public boolean canTakeItem(Container target, int index, ItemStack itemStack) {
        return target.hasAnyMatching(ItemStack::isEmpty) && !getBlockState().getValue(MusicBoxBlock.POWERED);
    }

    /**
     * Eject the note grid item from the music box.
     * If there is a container(NOT a worldly container) at the back of the music box, put the note grid item into it.
     * Otherwise, spawn the note grid item.
     */
    public void ejectNoteGrid(Level level, BlockPos blockPos, BlockState blockState) {
        Direction direction = blockState.getValue(MusicBoxBlock.FACING);
        Container container = HopperBlockEntity.getContainerAt(level, blockPos.relative(direction.getOpposite()));
        ItemStack itemStack = removeItem();
        if (container != null && !(container instanceof WorldlyContainer)) {
            int size = container.getContainerSize();
            for (int slot = 0; slot < size; ++slot) {
                if (container.getItem(slot).isEmpty()) {
                    container.setItem(slot, itemStack);
                    container.setChanged();
                    return;
                }
            }
        }
        Position position = blockPos.getCenter().relative(direction, 0.7D);
        DefaultDispenseItemBehavior.spawnItem(level, itemStack, 2, direction, position);
    }

    public int getSignal() {
        byte minNote = PLAYER.getMinNote();
        return minNote > 13 ? 15 : minNote + 2;
    }

    public void setTickPerBeat(Level level, BlockPos blockPos, byte tickPerBeat) {
        if (PLAYER.getTickPerBeat() == tickPerBeat) {
            return;
        }
        PLAYER.setTickPerBeat(tickPerBeat);
        // Sync the note grid player information to the client.
        if (level instanceof ServerLevel serverLevel) {
            BlockUtils.markForUpdate(serverLevel, blockPos);
        }
    }

    public byte getTickPerBeat() {
        return PLAYER.getTickPerBeat();
    }

    /**
     * Play next beat. From server to client there will both play their own next beat.
     */
    public void playNextBeat(Level level, BlockPos blockPos, BlockState blockState) {
        if (!blockState.getValue(MusicBoxBlock.HAS_NOTE_GRID) || blockState.getValue(MusicBoxBlock.POWERED) || level.isClientSide) {
            return;
        }
        if (level instanceof ServerLevel serverLevel) {
            // play next beat on server
            PLAYER.nextBeat(level, blockPos, blockState);
            // ask the client to play next beat
            playNextBeat = true;
            BlockUtils.markForUpdate(serverLevel, blockPos);
        }
    }

    /**
     * For creative mode only. Join the note grid data with the given item and save it to the note grid item.
     */
    public boolean joinData(ItemStack itemStack) {
        NoteGridData newData = null;
        if (itemStack.is(CCMain.NOTE_GRID_ITEM)) {
            newData = NoteGridData.ofNoteGrid(itemStack);
        } else if (itemStack.is(Items.WRITABLE_BOOK)) {
            newData = NoteGridData.ofBook(itemStack);
        }
        if (newData == null) {
            return false;
        }
        ItemStack noteGrid = removeItem();
        if (itemStack.hasCustomHoverName()) {
            noteGrid.setHoverName(itemStack.getHoverName());
        }
        NoteGridUtils.join(NoteGridData.ofNoteGrid(noteGrid), newData).saveToNoteGrid(noteGrid);
        setItem(noteGrid);
        return true;
    }

    @Override
    public void onFinish() {
        if (level instanceof ServerLevel serverLevel) {
            ejectNoteGrid(serverLevel, getBlockPos(), getBlockState());
        }
    }

    @Override
    public void onBeat() {
        setChanged();
    }

    @Override
    public void onPageChange() {
        if (level instanceof ServerLevel serverLevel) {
            BlockUtils.markForUpdate(serverLevel, getBlockPos());
        }
    }
}
