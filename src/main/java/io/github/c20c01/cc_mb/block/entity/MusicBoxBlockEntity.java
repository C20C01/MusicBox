package io.github.c20c01.cc_mb.block.entity;

import com.mojang.logging.LogUtils;
import io.github.c20c01.cc_mb.MusicBox;
import io.github.c20c01.cc_mb.block.MusicBoxBlock;
import io.github.c20c01.cc_mb.block.NoteGridBoxBlock;
import io.github.c20c01.cc_mb.client.NoteGridDataManager;
import io.github.c20c01.cc_mb.data.NoteGridData;
import io.github.c20c01.cc_mb.player.MusicBoxPlayer;
import io.github.c20c01.cc_mb.player.NoteGridTicker;
import io.github.c20c01.cc_mb.player.Octave;
import io.github.c20c01.cc_mb.player.SpeakerConfig;
import io.github.c20c01.cc_mb.util.EjectUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class MusicBoxBlockEntity extends NoteGridBoxBlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final MusicBoxPlayer player;

    public MusicBoxBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(MusicBox.MUSIC_BOX_BLOCK_ENTITY.get(), blockPos, blockState);
        this.player = new MusicBoxPlayer(this, this, worldPosition);
    }

    public static void tick(Level level, BlockPos ignoredBlockPos, BlockState ignoredBlockState, MusicBoxBlockEntity musicBox) {
        musicBox.player.tick(level);
    }

    @Override
    public void setItem(ItemStack itemStack) {
        super.setItem(itemStack);
        if (getData() == null) player.ticker.reset();
        syncPlayerData();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        player.loadAdditional(input);
        super.loadAdditional(input);// -> setItem() -> syncPlayerData() <- need player data loaded before syncing
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        player.saveAdditional(output);
        super.saveAdditional(output);
    }

    @Override
    public byte getMinNote() {
        return player.ticker.getMinNote();
    }

    @Override
    public void ejectNoteGrid(Level level, BlockPos blockPos, BlockState blockState, ItemStack noteGrid) {
        Direction front = blockState.getValue(MusicBoxBlock.FACING);
        EjectUtils.eject(level, blockPos, front.getOpposite(), front, noteGrid);
    }

    /**
     * Update the instrument of the music box according to the block below it.
     *
     * @param below the position below the music box
     */
    public void updateInstrumentFromBelow(Level level, BlockPos below) {
        if (level.getBlockEntity(below) instanceof SoundBoxBlockEntity soundBox) {
            updateInstrument(soundBox.getSoundLocation(), soundBox.getSoundSeed());
        } else {
            NoteBlockInstrument instrument = level.getBlockState(below).instrument();
            Holder<SoundEvent> soundEvent = instrument.worksAboveNoteBlock() ? NoteBlockInstrument.HARP.getSoundEvent() : instrument.getSoundEvent();
            updateInstrument(soundEvent.value().location(), null);
        }
    }

    private void updateInstrument(@Nullable Identifier soundLocation, @Nullable Long soundSeed) {
        SpeakerConfig config = player.config;
        if (!Objects.equals(soundLocation, config.getSoundLocation()) || !Objects.equals(soundSeed, config.getSeed())) {
            config.setSoundLocation(soundLocation);
            config.setNullableSeed(soundSeed);
            syncSoundData();
        }
    }

    public byte getTickPerBeat() {
        return player.ticker.getTickPerBeat();
    }

    public void setTickPerBeat(byte tickPerBeat) {
        NoteGridTicker ticker = player.ticker;
        if (ticker.getTickPerBeat() != tickPerBeat) {
            ticker.setTickPerBeat(tickPerBeat);
            syncPlayerData();
        }
    }

    public void cycleOctave(Level level, Player player) {
        int next;
        if (player.isSecondaryUseActive()) {
            // decrease octave
            next = getOctave() > Octave.MIN ? getOctave() - 1 : Octave.MAX;
        } else {
            // increase octave
            next = getOctave() < Octave.MAX ? getOctave() + 1 : Octave.MIN;
        }
        setOctave((byte) next);
        level.playSound(null, worldPosition, SoundEvents.SPYGLASS_USE, SoundSource.PLAYERS);
        player.sendOverlayMessage(Component.translatable(MusicBox.TEXT_CHANGE_OCTAVE).append(String.valueOf(next)).withStyle(ChatFormatting.DARK_AQUA));
    }

    public byte getOctave() {
        return player.config.getOctave();
    }

    private void setOctave(byte octave) {
        if (player.config.getOctave() != octave) {
            player.config.setOctave(octave);
            syncPlayerData();
        }
    }

    public void playNextBeat(Level level) {
        // sync before next beat to make sure the client play the same next beat as the server
        syncNextBeat();
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
        NoteGridData otherData = null;
        if (other.is(MusicBox.NOTE_GRID_ITEM.get())) {
            otherData = NoteGridData.ofNoteGrid(other);
        } else if (other.is(Items.WRITABLE_BOOK)) {
            otherData = NoteGridData.ofBook(other);
        }
        if (otherData == null) return null;

        ItemStack noteGrid = getItem().copy();
        if (other.has(DataComponents.CUSTOM_NAME)) noteGrid.set(DataComponents.CUSTOM_NAME, other.getHoverName());
        NoteGridData.ofNoteGrid(noteGrid).withDataMerged(otherData).saveToNoteGrid(noteGrid);
        return noteGrid;
    }

    @Override
    public void onPageChanged(int pageNum) {
        // no need to sync if the page change is caused by next beat
        if (getBlockState().getValue(NoteGridBoxBlock.POWERED)) syncPlayerData();
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

    // region sync data from server to client
    private void savePlayerData(TagValueOutput output) {
        NoteGridData data = getData();
        if (data != null) {
            player.saveSync(output);
            output.putInt("hash", data.hashCode());
        }
    }

    private void loadPlayerData(ValueInput input) {
        input.getInt("hash").ifPresentOrElse(
                hash -> {
                    player.loadSync(input);
                    NoteGridDataManager.getInstance().getNoteGridData(hash, getBlockPos(), this::setData);
                },
                () -> {
                    player.ticker.reset();
                    setData(null);
                }
        );
    }

    private void syncPlayerData() {
        syncToClient((registries) -> getUpdateTag(registries, output -> {
            output.putByte("type", (byte) 1);
            savePlayerData(output);
        }));
    }

    private void syncSoundData() {
        syncToClient((registries) -> getUpdateTag(registries, output -> {
            output.putByte("type", (byte) 2);
            player.saveSound(output);
        }));
    }

    private void syncNextBeat() {
        syncToClient((registries) -> getUpdateTag(registries, output -> {
            output.putByte("type", (byte) 3);
            player.saveSync(output);
        }));
    }

    private void syncToClient(Function<RegistryAccess, CompoundTag> updateTagSaver) {
        if (level instanceof ServerLevel serverLevel) {
            Packet<?> packet = ClientboundBlockEntityDataPacket.create(this, (_, registries) -> updateTagSaver.apply(registries));
            for (ServerPlayer player : serverLevel.getChunkSource().chunkMap.getPlayers(ChunkPos.containing(worldPosition), false)) {
                player.connection.send(packet);
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return getUpdateTag(registries, output -> {
            savePlayerData(output);
            player.saveSound(output);
        });
    }

    private CompoundTag getUpdateTag(HolderLookup.Provider registries, Consumer<TagValueOutput> tagWriter) {
        CompoundTag tag;
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(this.problemPath(), LOGGER)) {
            TagValueOutput output = TagValueOutput.createWithContext(reporter, registries);
            tagWriter.accept(output);
            tag = output.buildResult();
        }
        return tag;
    }

    @Override
    public void handleUpdateTag(ValueInput input) {
        switch (input.getByteOr("type", (byte) 0)) {
            case 1 -> loadPlayerData(input);
            case 2 -> player.loadSound(input);
            case 3 -> {
                player.loadSync(input);
                player.nextBeat(level);
            }
            default -> {
                loadPlayerData(input);
                player.loadSound(input);
            }
        }
    }

    @Override
    public void onDataPacket(Connection net, ValueInput input) {
        handleUpdateTag(input);
    }
    // endregion
}
