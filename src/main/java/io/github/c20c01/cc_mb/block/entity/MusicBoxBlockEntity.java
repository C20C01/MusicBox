package io.github.c20c01.cc_mb.block.entity;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.block.MusicBoxBlock;
import io.github.c20c01.cc_mb.client.NoteGridDataManager;
import io.github.c20c01.cc_mb.data.NoteGridData;
import io.github.c20c01.cc_mb.util.BlockUtils;
import io.github.c20c01.cc_mb.util.NoteGridUtils;
import io.github.c20c01.cc_mb.util.player.MusicBoxPlayer;
import io.github.c20c01.cc_mb.util.player.Sounder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.Optional;

public class MusicBoxBlockEntity extends BaseBoxPlayerBlockEntity<MusicBoxPlayer> {
    private final MusicBoxPlayer PLAYER;
    private boolean playNextBeat = false; // whether ask the client to play next beat

    public MusicBoxBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(CCMain.MUSIC_BOX_BLOCK_ENTITY.get(), blockPos, blockState);
        PLAYER = new MusicBoxPlayer(this);
    }

    public static void tick(Level level, BlockPos blockPos, BlockState blockState, MusicBoxBlockEntity blockEntity) {
        blockEntity.PLAYER.tick(level, blockPos, blockState);
    }

    @Override
    MusicBoxPlayer getPlayer() {
        return PLAYER;
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
        if (tag.contains("note_grid_hash")) {
            // has note grid, load data and play next beat if needed
            NoteGridDataManager.getInstance().getNoteGridData(tag.getInt("note_grid_hash"), getBlockPos(), d -> PLAYER.data = d);
            if (tag.getBoolean("play_next_beat")) {
                PLAYER.nextBeat(level, getBlockPos(), getBlockState());
            }
        } else {
            // no note grid, remove data
            NoteGridData data = PLAYER.data;
            if (data != null) {
                NoteGridDataManager.getInstance().markRemovable(data.hashCode());
                PLAYER.data = null;
            }
        }
        // update the player's state
        PLAYER.loadUpdateTag(tag);
    }

    public Optional<NoteGridData> getPlayerData() {
        return Optional.ofNullable(PLAYER.data);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        PLAYER.saveUpdateTag(tag);
        NoteGridData data = PLAYER.data;
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
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide && PLAYER.data != null) {
            NoteGridDataManager.getInstance().markRemovable(PLAYER.data.hashCode());
        }
    }

    @Override
    protected void loadItem(ItemStack noteGrid) {
        PLAYER.data = NoteGridData.ofNoteGrid(noteGrid);
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

    public byte getTickPerBeat() {
        return PLAYER.ticker.getTickPerBeat();
    }

    public void setTickPerBeat(ServerLevel level, BlockPos blockPos, byte tickPerBeat) {
        if (PLAYER.ticker.getTickPerBeat() == tickPerBeat) {
            return;
        }
        PLAYER.ticker.setTickPerBeat(tickPerBeat);
        // Sync the note grid player information to the client.
        BlockUtils.markForUpdate(level, blockPos);
    }

    public byte getOctave() {
        return PLAYER.sounder.getOctave();
    }

    public void setOctave(ServerLevel level, BlockPos blockPos, net.minecraft.world.entity.player.Player player) {
        int next;
        if (player.isSecondaryUseActive()) {
            // decrease octave
            next = getOctave() > Sounder.MIN_OCTAVE ? getOctave() - 1 : Sounder.MAX_OCTAVE;
        } else {
            // increase octave
            next = getOctave() < Sounder.MAX_OCTAVE ? getOctave() + 1 : Sounder.MIN_OCTAVE;
        }
        setOctave(level, blockPos, (byte) next);
        level.playSound(null, blockPos, SoundEvents.SPYGLASS_USE, SoundSource.BLOCKS);
        player.displayClientMessage(Component.translatable(CCMain.TEXT_CHANGE_OCTAVE).append(String.valueOf(next)).withStyle(ChatFormatting.DARK_AQUA), true);
    }

    private void setOctave(ServerLevel level, BlockPos blockPos, byte octave) {
        if (PLAYER.sounder.getOctave() == octave) {
            return;
        }
        PLAYER.sounder.setOctave(octave);
        // Sync the note grid player information to the client.
        BlockUtils.markForUpdate(level, blockPos);
    }

    /**
     * Play next beat. From server to client there will both play their own next beat.
     */
    public void playNextBeat(ServerLevel level, BlockPos blockPos, BlockState blockState) {
        if (!blockState.getValue(MusicBoxBlock.HAS_NOTE_GRID) || blockState.getValue(MusicBoxBlock.POWERED)) {
            return;
        }
        // play next beat on server
        PLAYER.nextBeat(level, blockPos, blockState);
        // ask the client to play next beat
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
    public void onPageChange(Level level, BlockPos blockPos) {
        if (level instanceof ServerLevel serverLevel) {
            BlockUtils.markForUpdate(serverLevel, blockPos);
        }
    }
}
