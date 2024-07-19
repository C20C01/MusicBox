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
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

// 重写，尚未完全实现

public class MusicBoxBlockEntity$ extends AbstractItemLoaderBlockEntity implements PlayerListener {
    public static final String NOTE_GRID = "NoteGrid";
    private static final byte BEAT_PER_SYNC_ON_BEAT = 20;
    private byte beatSinceLastSyncOnBeat;
    private final NoteGridPlayer PLAYER;

    public MusicBoxBlockEntity$(BlockPos blockPos, BlockState blockState) {
        super(CCMain.MUSIC_BOX_BLOCK_ENTITY.get(), blockPos, blockState, NOTE_GRID);
        PLAYER = new NoteGridPlayer(this);
    }

    public static void tick(Level level, BlockPos blockPos, BlockState blockState, MusicBoxBlockEntity$ blockEntity) {
        blockEntity.PLAYER.tick(level, blockPos, blockState);
    }

    @Override
    public void load(CompoundTag compoundTag) {
        super.load(compoundTag);
        PLAYER.load(compoundTag);
        System.out.println("Load in: " + level + " size: " + compoundTag.sizeInBytes());
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag) {
        super.saveAdditional(compoundTag);
        PLAYER.saveAdditional(compoundTag);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    protected void loadItem(ItemStack noteGrid) {
        Level level = getLevel();
        if (level == null) {
            return;
        }
        Integer noteGridId = NoteGrid.getId(noteGrid);
        if (noteGridId == null) {
            return;
        }
        if (level instanceof ServerLevel serverLevel) {
            NoteGridData$ noteGridData = NoteGridData$.ofId(serverLevel.getServer(), noteGridId);
            PLAYER.setNoteGridData(noteGridData);
        } else {
            NoteGridData$ noteGridData = NoteGridData$.ofId(noteGridId, PLAYER::setNoteGridData);
            PLAYER.setNoteGridData(noteGridData);
        }
        System.out.println("Load item in: " + level);
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
        BlockUtil.changeProperty(level, getBlockPos(), getBlockState(), MusicBoxBlock.HAS_NOTE_GRID, false);
        setChanged(level, getBlockPos(), getBlockState());
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack itemStack) {
        return itemStack.is(CCMain.NOTE_GRID_ITEM.get()) && this.getItem(slot).isEmpty();
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
        if (level instanceof ServerLevel serverLevel && beatSinceLastSyncOnBeat++ >= BEAT_PER_SYNC_ON_BEAT) {
            beatSinceLastSyncOnBeat = 0;
            serverLevel.getChunkSource().blockChanged(blockPos);
        }
    }
}
