package io.github.c20c01.cc_mb.block.entity;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.block.MusicBoxBlock;
import io.github.c20c01.cc_mb.client.NoteGridDataManager;
import io.github.c20c01.cc_mb.data.NoteGridData;
import io.github.c20c01.cc_mb.util.BlockUtils;
import io.github.c20c01.cc_mb.util.NoteGridUtils;
import io.github.c20c01.cc_mb.util.player.AbstractNoteGridPlayer;
import io.github.c20c01.cc_mb.util.player.MusicBoxPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Position;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import javax.annotation.Nullable;
import java.util.Optional;

public class MusicBoxBlockEntity extends AbstractItemLoaderBlockEntity implements MusicBoxPlayer.Listener {
    public static final String NOTE_GRID = "note_grid";
    private final MusicBoxPlayer PLAYER;
    private boolean playNextBeat = false; // whether ask the client to play next beat

    public MusicBoxBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(CCMain.MUSIC_BOX_BLOCK_ENTITY.get(), blockPos, blockState, NOTE_GRID);
        PLAYER = new MusicBoxPlayer(this);
    }

    public static void tick(Level level, BlockPos blockPos, BlockState blockState, MusicBoxBlockEntity blockEntity) {
        blockEntity.PLAYER.update(level, blockPos, blockState);
        blockEntity.PLAYER.tick();
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        PLAYER.load(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        PLAYER.saveAdditional(output);
    }

    @Override
    public void onDataPacket(Connection net, ValueInput input) {
        handleUpdateTag(input);
    }

    @Override
    public void handleUpdateTag(ValueInput input) {
        Optional<Integer> hash = input.getInt("note_grid_hash");
        if (hash.isPresent()) {
            // has note grid, load data and play next beat if needed
            NoteGridDataManager.getInstance().getNoteGridData(hash.get(), getBlockPos(), PLAYER::setData);
            if (input.getBooleanOr("play_next_beat", false)) {
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
        PLAYER.loadUpdateTag(input);
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
    // Client side only
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide && PLAYER.getData() != null) {
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
        Container container = HopperBlockEntity.getContainerOrHandlerAt(level, blockPos.relative(direction.getOpposite()), direction).container();
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

    public void setTickPerBeat(ServerLevel level, BlockPos blockPos, byte tickPerBeat) {
        if (PLAYER.getTickPerBeat() == tickPerBeat) {
            return;
        }
        PLAYER.setTickPerBeat(tickPerBeat);
        // Sync the note grid player information to the client.
        BlockUtils.markForUpdate(level, blockPos);
    }

    public byte getTickPerBeat() {
        return PLAYER.getTickPerBeat();
    }

    public void setOctave(ServerLevel level, BlockPos blockPos, Player player) {
        int next;
        if (player.isSecondaryUseActive()) {
            // decrease octave
            next = getOctave() > AbstractNoteGridPlayer.MIN_OCTAVE ? getOctave() - 1 : AbstractNoteGridPlayer.MAX_OCTAVE;
        } else {
            // increase octave
            next = getOctave() < AbstractNoteGridPlayer.MAX_OCTAVE ? getOctave() + 1 : AbstractNoteGridPlayer.MIN_OCTAVE;
        }
        setOctave(level, blockPos, (byte) next);
        level.playSound(null, blockPos, SoundEvents.SPYGLASS_USE, SoundSource.BLOCKS);
        player.displayClientMessage(Component.translatable(CCMain.TEXT_CHANGE_OCTAVE).append(String.valueOf(next)).withStyle(ChatFormatting.DARK_AQUA), true);
    }

    private void setOctave(ServerLevel level, BlockPos blockPos, byte octave) {
        if (PLAYER.getOctave() == octave) {
            return;
        }
        PLAYER.setOctave(octave);
        // Sync the note grid player information to the client.
        BlockUtils.markForUpdate(level, blockPos);
    }

    public byte getOctave() {
        return PLAYER.getOctave();
    }

    /**
     * Play next beat. From server to client there will both play their own next beat.
     */
    public void playNextBeat(ServerLevel level, BlockPos blockPos, BlockState blockState) {
        if (!blockState.getValue(MusicBoxBlock.HAS_NOTE_GRID) || blockState.getValue(MusicBoxBlock.POWERED)) {
            return;
        }
        // play next beat on server (only for the level event)
        PLAYER.nextBeat(level, blockPos, blockState);
        // ask the client to play next beat (for the sound and particles)
        playNextBeat = true;
        BlockUtils.markForUpdate(level, blockPos);
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
