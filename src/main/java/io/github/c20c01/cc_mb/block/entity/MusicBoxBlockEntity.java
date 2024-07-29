package io.github.c20c01.cc_mb.block.entity;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.block.MusicBoxBlock;
import io.github.c20c01.cc_mb.data.Beat;
import io.github.c20c01.cc_mb.data.NoteGridData;
import io.github.c20c01.cc_mb.network.CCNetwork;
import io.github.c20c01.cc_mb.network.MusicBoxSyncRequestPacket;
import io.github.c20c01.cc_mb.util.BlockUtils;
import io.github.c20c01.cc_mb.util.NoteGridUtils;
import io.github.c20c01.cc_mb.util.player.MusicBoxPlayer;
import io.github.c20c01.cc_mb.util.player.PlayerListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class MusicBoxBlockEntity extends AbstractItemLoaderBlockEntity implements PlayerListener, LazyUpdateBlockEntity {
    public static final String NOTE_GRID = "NoteGrid";
    private final MusicBoxPlayer PLAYER;

    public MusicBoxBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(CCMain.MUSIC_BOX_BLOCK_ENTITY.get(), blockPos, blockState, NOTE_GRID);
        PLAYER = new MusicBoxPlayer(this);
    }

    public static void tick(Level level, BlockPos blockPos, BlockState blockState, MusicBoxBlockEntity blockEntity) {
        blockEntity.PLAYER.tick(level, blockPos, blockState);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        PLAYER.load(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        PLAYER.saveAdditional(tag);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return getCommonUpdateTag();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return createCommonUpdatePacket();
    }

    @Override
    public CompoundTag getLazyUpdateTag() {
        CompoundTag tag = new CompoundTag();
        super.saveAdditional(tag);
        return tag;
    }

    @Override
    public boolean isLazyUpdateTag(CompoundTag tag) {
        return tag.contains(NOTE_GRID);
    }

    @Override
    public void handleLazyUpdateTag(CompoundTag tag) {
        setItem(ItemStack.of(tag.getCompound(NOTE_GRID)));
    }

    @Override
    public CompoundTag getCommonUpdateTag() {
        CompoundTag tag = new CompoundTag();
        PLAYER.getUpdateTag(tag);
        return tag;
    }

    @Override
    public boolean shouldRequestLazyUpdate() {
        boolean emptyNoteGrid = getItem().isEmpty();
        boolean shouldHaveNoteGrid = getBlockState().getValue(MusicBoxBlock.HAS_NOTE_GRID);
        return emptyNoteGrid == shouldHaveNoteGrid;
    }

    @Override
    public void sendLazyUpdateRequest() {
        CCNetwork.CHANNEL.sendToServer(new MusicBoxSyncRequestPacket(getBlockPos()));
    }

    @Override
    public void handleCommonUpdateTag(CompoundTag tag) {
        PLAYER.handleUpdateTag(tag);
    }

    @Override
    protected void loadItem(ItemStack noteGrid) {
        PLAYER.setNoteGridData(NoteGridData.ofNoteGrid(noteGrid));
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
            PLAYER.nextBeat(serverLevel, blockPos, blockState, false);
            BlockUtils.markForUpdate(serverLevel, blockPos);
        }
    }

    /**
     * For creative mode only. Join the note grid data with the given data and save it to the note grid item.
     */
    public void joinData(NoteGridData data) {
        ItemStack noteGrid = removeItem();
        NoteGridUtils.join(NoteGridData.ofNoteGrid(noteGrid), data).saveToNoteGrid(noteGrid);
        setItem(noteGrid);
    }

    @Override
    public void onFinish(Level level, BlockPos blockPos, BlockState blockState) {
        ejectNoteGrid(level, blockPos, blockState);
    }

    @Override
    public boolean onBeat(Level level, BlockPos blockPos, BlockState blockState, Beat lastBeat, Beat currentBeat) {
        level.blockEntityChanged(blockPos);
        if (currentBeat.getMinNote() != lastBeat.getMinNote()) {
            level.updateNeighbourForOutputSignal(blockPos, blockState.getBlock());
        }
        return false;
    }

    @Override
    public void onPageChange(Level level, BlockPos blockPos) {
        if (level instanceof ServerLevel serverLevel) {
            BlockUtils.markForUpdate(serverLevel, blockPos);
        }
    }
}
