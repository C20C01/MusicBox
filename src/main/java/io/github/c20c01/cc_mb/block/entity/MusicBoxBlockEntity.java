package io.github.c20c01.cc_mb.block.entity;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.block.BlockUtil;
import io.github.c20c01.cc_mb.block.MusicBoxBlock;
import io.github.c20c01.cc_mb.data.Beat;
import io.github.c20c01.cc_mb.data.NoteGridData$;
import io.github.c20c01.cc_mb.item.NoteGrid;
import io.github.c20c01.cc_mb.util.player.NoteGridPlayer;
import io.github.c20c01.cc_mb.util.player.PlayerListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class MusicBoxBlockEntity extends AbstractItemLoaderBlockEntity implements PlayerListener {
    public static final String NOTE_GRID = "NoteGrid";
    private static final byte BEAT_PER_UPDATE = 20;
    private final NoteGridPlayer PLAYER;
    private byte beatSinceLastUpdate;
    private Integer noteGridId = null;// cache the note grid id, to avoid unnecessary updates

    public MusicBoxBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(CCMain.MUSIC_BOX_BLOCK_ENTITY.get(), blockPos, blockState, NOTE_GRID);
        PLAYER = new NoteGridPlayer(this);
    }

    public static void tick(Level level, BlockPos blockPos, BlockState blockState, MusicBoxBlockEntity blockEntity) {
        blockEntity.PLAYER.tick(level, blockPos, blockState);
    }

    @Override
    public void load(CompoundTag compoundTag) {
        super.load(compoundTag);
        PLAYER.load(compoundTag);
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag) {
        super.saveAdditional(compoundTag);
        PLAYER.saveAdditional(compoundTag);
    }

    @Override
    public void setLevel(Level pLevel) {
        super.setLevel(pLevel);
        // Initialize the note grid data.
        setNoteGridId(NoteGrid.getId(getItem()));
    }

    /**
     * Handle the update packet from the server.
     * It will sync the note grid id and the player information.
     */
    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            PLAYER.loadUpdateTag(tag);
            setNoteGridId(tag.contains(NoteGrid.NOTE_GRID_ID) ? tag.getInt(NoteGrid.NOTE_GRID_ID) : null);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        PLAYER.saveUpdateTag(tag);
        Integer noteGridId = NoteGrid.getId(getItem());
        if (noteGridId != null) {
            tag.putInt(NoteGrid.NOTE_GRID_ID, noteGridId);
        }
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * Set the note grid id. If the id is changed, load or request the note grid data.
     */
    private void setNoteGridId(@Nullable Integer id) {
        Integer oldId = noteGridId;
        noteGridId = id;
        if (id == null || id.equals(oldId)) {
            return;
        }
        if (level instanceof ServerLevel serverLevel) {
            // Load from the server's data storage.
            NoteGridData$ noteGridData = NoteGridData$.ofId(serverLevel.getServer(), id);
            PLAYER.setNoteGridData(noteGridData);
        } else {
            // Load from the client's cache and send a request to the server to get the latest data.
            NoteGridData$ noteGridData = NoteGridData$.ofId(id, PLAYER::setNoteGridData);
            PLAYER.setNoteGridData(noteGridData);
        }
    }

    @Override
    protected void loadItem(ItemStack noteGrid) {
        Level level = getLevel();
        if (level == null) {
            return;
        }
        setNoteGridId(NoteGrid.getId(noteGrid));
        BlockUtil.changeProperty(level, getBlockPos(), getBlockState(), MusicBoxBlock.HAS_NOTE_GRID, true);
        setChanged(level, getBlockPos(), getBlockState());
    }

    @Override
    protected void unloadItem() {
        Level level = getLevel();
        if (level == null) {
            return;
        }
        PLAYER.reset();
        noteGridId = null;
        BlockUtil.changeProperty(level, getBlockPos(), getBlockState(), MusicBoxBlock.HAS_NOTE_GRID, false);
        setChanged(level, getBlockPos(), getBlockState());
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack itemStack) {
        return itemStack.is(CCMain.NOTE_GRID_ITEM.get()) && NoteGrid.getId(itemStack) != null && this.getItem(slot).isEmpty();
    }

    /**
     * Eject the note grid item from the music box.
     * If there is a container(NOT a worldly container) at the back of the music box, put the note grid item into it.
     * Otherwise, spawn the note grid item.
     */
    private void ejectNoteGrid(Level level, BlockPos blockPos, BlockState blockState) {
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
            serverLevel.getChunkSource().blockChanged(blockPos);
        }
    }

    public byte getTickPerBeat() {
        return PLAYER.getTickPerBeat();
    }

    public void playOneBeat(Level level, BlockPos blockPos, BlockState blockState) {
        if (blockState.getValue(MusicBoxBlock.HAS_NOTE_GRID) && !blockState.getValue(MusicBoxBlock.POWERED)) {
            PLAYER.nextBeat(level, blockPos, blockState);
        }
    }

    @Override
    public void onFinish(Level level, BlockPos blockPos, BlockState blockState) {
        ejectNoteGrid(level, blockPos, blockState);
    }

    @Override
    public void onBeat(Level level, BlockPos blockPos, BlockState blockState, Beat lastBeat, Beat currentBeat) {
        level.blockEntityChanged(blockPos);
        if (currentBeat.getMinNote() != lastBeat.getMinNote()) {
            level.updateNeighbourForOutputSignal(blockPos, blockState.getBlock());
        }
        // Sync the note grid player information to the client every BEAT_PER_SYNC_ON_BEAT beats.
        if (level instanceof ServerLevel serverLevel && beatSinceLastUpdate++ >= BEAT_PER_UPDATE) {
            beatSinceLastUpdate = 0;
            serverLevel.getChunkSource().blockChanged(blockPos);
        }
    }
}
