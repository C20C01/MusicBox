package io.github.c20c01.cc_mb.block.entity;

import com.mojang.logging.LogUtils;
import io.github.c20c01.cc_mb.MusicBox;
import io.github.c20c01.cc_mb.block.MusicBoxBlock;
import io.github.c20c01.cc_mb.block.NoteGridBoxBlock;
import io.github.c20c01.cc_mb.client.NoteGridDataManager;
import io.github.c20c01.cc_mb.data.NoteGridData;
import io.github.c20c01.cc_mb.network.sync.BlockEntityDataSyncer;
import io.github.c20c01.cc_mb.network.sync.BlockEntitySyncDataType;
import io.github.c20c01.cc_mb.player.MusicBoxPlayer;
import io.github.c20c01.cc_mb.util.EjectUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.List;

public class MusicBoxBlockEntity extends NoteGridBoxBlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final MusicBoxPlayer player;
    private final MusicBoxBlockEntityDataSyncer syncer;

    public MusicBoxBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(MusicBox.MUSIC_BOX_BLOCK_ENTITY.get(), blockPos, blockState);
        this.player = new MusicBoxPlayer(this, this, worldPosition);// use worldPosition because the blockPos is mutable.
        this.syncer = new MusicBoxBlockEntityDataSyncer();
    }

    public static void tick(Level level, BlockPos ignoredBlockPos, BlockState ignoredBlockState, MusicBoxBlockEntity musicBox) {
        if (musicBox.hasData()) musicBox.player.tick(level);
    }

    @Override
    public void setItem(ItemStack itemStack) {
        if (itemStack.isEmpty()) player.reset();
        super.setItem(itemStack);
        syncer.sync(this, MusicBoxBlockEntityDataSyncer.PLAYER_DATA);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        player.loadAdditional(input);
        syncer.markDirty(MusicBoxBlockEntityDataSyncer.SOUND_DATA);
        super.loadAdditional(input);// -> setItem -> sync <- make player data ready and mark sound data dirty first
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        player.saveAdditional(output);
        super.saveAdditional(output);
    }

    @Override
    public byte getMinNote() {
        return player.getMinNote();
    }

    @Override
    public void ejectNoteGrid(Level level, BlockPos blockPos, BlockState blockState, ItemStack noteGrid) {
        // back container -> front container -> front item drop
        Direction front = blockState.getValue(MusicBoxBlock.FACING);
        if (EjectUtils.tryToContainer(level, blockPos, front.getOpposite(), noteGrid)) return;
        if (EjectUtils.tryToContainer(level, blockPos, front, noteGrid)) return;
        EjectUtils.toWorld(level, blockPos, front, noteGrid);
    }

    /**
     * Update the instrument of the music box according to the block below it.
     */
    public void updateInstrumentFromBelow(LevelReader level, BlockPos musicBoxPos) {
        BlockPos below = musicBoxPos.below();
        if (level.getBlockEntity(below) instanceof SoundBoxBlockEntity soundBox) {
            updateInstrument(soundBox.getSoundLocation(), soundBox.getSoundSeed());
        } else {
            NoteBlockInstrument instrument = level.getBlockState(below).instrument();
            Holder<SoundEvent> soundEvent = instrument.worksAboveNoteBlock() ? NoteBlockInstrument.HARP.getSoundEvent() : instrument.getSoundEvent();
            updateInstrument(soundEvent.value().location(), null);
        }
    }

    /**
     * Update the instrument of the music box according to the given sound location and seed.
     */
    public void updateInstrument(@Nullable Identifier soundLocation, @Nullable Long soundSeed) {
        if (player.tryToUpdateInstrument(soundLocation, soundSeed)) {
            syncer.sync(this, MusicBoxBlockEntityDataSyncer.SOUND_DATA);
        }
    }

    public byte getTickPerBeat() {
        return player.getTickPerBeat();
    }

    public void setTickPerBeat(byte tickPerBeat) {
        if (player.tryToSetTickPerBeat(tickPerBeat)) {
            syncer.sync(this, MusicBoxBlockEntityDataSyncer.PLAYER_DATA);
        }
    }

    public void cycleOctave(Level level, Player player) {
        byte newOctave = this.player.cycleOctave(player.isSecondaryUseActive());
        syncer.sync(this, MusicBoxBlockEntityDataSyncer.PLAYER_DATA);
        level.playSound(null, worldPosition, SoundEvents.SPYGLASS_USE, SoundSource.PLAYERS);
        player.sendOverlayMessage(Component.translatable(MusicBox.TEXT_CHANGE_OCTAVE).append(String.valueOf(newOctave)).withStyle(ChatFormatting.DARK_AQUA));
    }

    public void playNextBeat(Level level) {
        // sync before next beat to make sure the client play the same next beat as the server
        syncer.sync(this, MusicBoxBlockEntityDataSyncer.NEXT_BEAT);
        player.nextBeat(level);
    }

    /**
     * For creative mode only. Create a new note grid item with the data that
     * merged from the data in the given item to the data in the music box, and return it.
     *
     * @return the note grid item with merged data, or null if the given item has no data to merge
     */
    @Nullable
    public ItemStack createNoteGridMerged(ItemStack other) {
        NoteGridData otherData = NoteGridData.ofItemStack(other);
        if (otherData == null) return null;

        ItemStack noteGrid = getItem().copy();
        if (other.has(DataComponents.CUSTOM_NAME)) noteGrid.set(DataComponents.CUSTOM_NAME, other.getHoverName());
        NoteGridData.ofNoteGrid(noteGrid).withDataMerged(otherData).saveToNoteGrid(noteGrid);
        return noteGrid;
    }

    @Override
    public void onPageChanged() {
        // no need to sync if the page change is caused by next beat
        if (getBlockState().getValue(NoteGridBoxBlock.POWERED)) {
            syncer.sync(this, MusicBoxBlockEntityDataSyncer.PLAYER_DATA);
        }
    }

    @Override
    // Client side only
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide()) {
            NoteGridData data = getData();
            if (data != null) NoteGridDataManager.getInstance().markRemovable(data.hashCode());
        }
    }

    private void savePlayerData(ValueOutput output) {
        NoteGridData data = getData();
        if (data != null) {
            player.saveSync(output);
            output.putInt("hash", data.hashCode());
        }
    }

    private void loadPlayerData(ValueInput input) {
        Integer hash = input.getInt("hash").orElse(null);
        if (hash == null) {
            player.reset();
            setData(null);
        } else {
            player.loadSync(input);
            NoteGridDataManager.getInstance().getNoteGridData(hash, getBlockPos(), this::setData);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return BlockEntityDataSyncer.getUpdateTag(problemPath(), LOGGER, registries, output -> {
            savePlayerData(output);
            player.saveSound(output);
        });
    }

    @Override
    public void handleUpdateTag(ValueInput input) {
        loadPlayerData(input);
        player.loadSound(input);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return syncer.getUpdatePacket(this, LOGGER);
    }

    @Override
    public void onDataPacket(Connection net, ValueInput input) {
        syncer.handleUpdatePacket(this, input);
    }

    // region sync data from server to client
    public static class MusicBoxBlockEntityDataSyncer extends BlockEntityDataSyncer<MusicBoxBlockEntity> {
        public static final BlockEntitySyncDataType<MusicBoxBlockEntity> PLAYER_DATA = new BlockEntitySyncDataType<>(0) {
            @Override
            public void writeData(MusicBoxBlockEntity blockEntity, ValueOutput output) {
                blockEntity.savePlayerData(output);
            }

            @Override
            public void readData(MusicBoxBlockEntity blockEntity, ValueInput input) {
                blockEntity.loadPlayerData(input);
            }
        };
        public static final BlockEntitySyncDataType<MusicBoxBlockEntity> SOUND_DATA = new BlockEntitySyncDataType<>(1) {
            @Override
            public void writeData(MusicBoxBlockEntity blockEntity, ValueOutput output) {
                blockEntity.player.saveSound(output);
            }

            @Override
            public void readData(MusicBoxBlockEntity blockEntity, ValueInput input) {
                blockEntity.player.loadSound(input);
            }
        };
        public static final BlockEntitySyncDataType<MusicBoxBlockEntity> NEXT_BEAT = new BlockEntitySyncDataType<>(2) {
            @Override
            public void writeData(MusicBoxBlockEntity blockEntity, ValueOutput output) {
                blockEntity.player.saveSync(output);
            }

            @Override
            public void readData(MusicBoxBlockEntity blockEntity, ValueInput input) {
                blockEntity.player.loadSync(input);
                blockEntity.player.nextBeat(blockEntity.level);
            }
        };

        public static final List<BlockEntitySyncDataType<MusicBoxBlockEntity>> ALL_TYPES = List.of(PLAYER_DATA, SOUND_DATA, NEXT_BEAT);

        @Override
        public List<BlockEntitySyncDataType<MusicBoxBlockEntity>> getAllTypes() {
            return ALL_TYPES;
        }
    }
    // endregion
}
