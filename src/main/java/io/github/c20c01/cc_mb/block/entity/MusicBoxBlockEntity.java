package io.github.c20c01.cc_mb.block.entity;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.block.MusicBoxBlock;
import io.github.c20c01.cc_mb.client.NoteGridDataManager;
import io.github.c20c01.cc_mb.data.NoteGridData;
import io.github.c20c01.cc_mb.util.BlockUtils;
import io.github.c20c01.cc_mb.util.NoteGridUtils;
import io.github.c20c01.cc_mb.util.player.MusicBoxPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Position;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.Optional;

public class MusicBoxBlockEntity extends AbstractItemLoaderBlockEntity implements MusicBoxPlayer.Listener {
    public static final String NOTE_GRID = "note_grid";
    private final MusicBoxPlayer PLAYER;

    public MusicBoxBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(CCMain.MUSIC_BOX_BLOCK_ENTITY.get(), blockPos, blockState, NOTE_GRID);
        PLAYER = new MusicBoxPlayer(this);
    }

    public static void tick(Level level, BlockPos blockPos, BlockState blockState, MusicBoxBlockEntity blockEntity) {
        if (level.isClientSide) {
            blockEntity.PLAYER.update(level, blockPos, blockState);
        }
        blockEntity.PLAYER.tick();
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        super.loadAdditional(tag, lookupProvider);
        PLAYER.load(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        super.saveAdditional(tag, lookupProvider);
        PLAYER.saveAdditional(tag);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookupProvider) {
        CompoundTag tag = pkt.getTag();
        if (!tag.isEmpty()) {
            handleUpdateTag(tag, lookupProvider);
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        PLAYER.loadUpdateTag(tag);
        if (tag.contains("note_grid_hash")) {
            NoteGridDataManager.getInstance().getNoteGridData(tag.getInt("note_grid_hash"), getBlockPos(), PLAYER::setData);
        } else {
            NoteGridData data = PLAYER.getData();
            if (data != null) {
                NoteGridDataManager.getInstance().markRemovable(data.hashCode());
                PLAYER.setData(null);
            }
        }
    }

    public Optional<NoteGridData> getPlayerData() {
        return Optional.ofNullable(PLAYER.getData());
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        PLAYER.saveUpdateTag(tag);
        NoteGridData data = PLAYER.getData();
        if (data != null) {
            tag.putInt("note_grid_hash", data.hashCode());
        }
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
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
        return itemStack.is(CCMain.NOTE_GRID_ITEM.get());
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
     * Play one beat. The sound and particle will be sent to the client from the server.
     * The effect will be affected by the network latency.
     */
    public void playOneBeat(Level level, BlockPos blockPos, BlockState blockState) {
        if (!blockState.getValue(MusicBoxBlock.HAS_NOTE_GRID) || blockState.getValue(MusicBoxBlock.POWERED)) {
            return;
        }
        if (level instanceof ServerLevel serverLevel) {
            PLAYER.hitOneBeat(serverLevel, blockPos, blockState);
            BlockUtils.markForUpdate(serverLevel, blockPos);
        }
    }

    /**
     * For creative mode only. Join the note grid data with the given item and save it to the note grid item.
     */
    public boolean joinData(ItemStack itemStack) {
        NoteGridData newData = null;
        if (itemStack.is(CCMain.NOTE_GRID_ITEM.get())) {
            newData = NoteGridData.ofNoteGrid(itemStack);
        } else if (itemStack.is(Items.WRITABLE_BOOK)) {
            newData = NoteGridData.ofBook(itemStack);
        }
        if (newData == null) {
            return false;
        }
        ItemStack noteGrid = removeItem();
        if (itemStack.has(DataComponents.CUSTOM_NAME)) {
            noteGrid.set(DataComponents.CUSTOM_NAME, itemStack.getHoverName());
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
