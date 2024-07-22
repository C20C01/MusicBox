package io.github.c20c01.cc_mb.util.player;

import io.github.c20c01.cc_mb.block.MusicBoxBlock;
import io.github.c20c01.cc_mb.block.entity.SoundBoxBlockEntity;
import io.github.c20c01.cc_mb.data.Beat;
import io.github.c20c01.cc_mb.data.NoteGridData;
import io.github.c20c01.cc_mb.data.Page;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

public class NoteGridPlayer {
    public static final byte MIN_TICK_PER_BEAT = 1;
    public static final byte MAX_TICK_PER_BEAT = 20;
    private static final Beat EMPTY_BEAT = new Beat();// Read only, used to avoid creating new object
    private static final float[] PITCHES = new float[25];

    static {
        for (int i = 0; i < PITCHES.length; i++) {
            PITCHES[i] = NoteBlock.getPitchFromNote(i);
        }
    }

    private final PlayerListener LISTENER;
    private byte tickPerBeat = getDefaultTickPerBeat();
    private byte tickSinceLastBeat;
    private byte beatNumber;
    private byte pageNumber;
    private Beat beat = new Beat();
    private NoteGridData noteGridData = null;

    public NoteGridPlayer(PlayerListener listener) {
        LISTENER = listener;
    }

    /**
     * Play the beat on the client or server side.
     */
    private static void playBeat(Level level, BlockPos blockPos, BlockState blockState, Beat beat) {
        if (beat.isEmpty()) {
            return;
        }
        NoteBlockInstrument instrument = blockState.getValue(MusicBoxBlock.INSTRUMENT);
        Holder<SoundEvent> soundEvent;
        long soundSeed;
        // Sound event & Sound seed
        if (instrument.hasCustomSound()) {
            if (level.getBlockEntity(blockPos.below()) instanceof SoundBoxBlockEntity blockEntity) {
                if (blockEntity.isSilent()) {
                    return;
                }
                soundEvent = blockEntity.getInstrument();
                soundSeed = blockEntity.getSoundSeed();
            } else {
                return;
            }
        } else {
            soundEvent = instrument.getSoundEvent();
            soundSeed = level.random.nextLong();
        }
        if (level.isClientSide) {
            playBeatOnClient((ClientLevel) level, blockPos, soundEvent, soundSeed, beat);
        } else {
            playBeatOnServer((ServerLevel) level, blockPos, soundEvent, soundSeed, beat);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void playBeatOnClient(ClientLevel level, BlockPos blockPos, Holder<SoundEvent> sound, long seed, Beat beat) {
        Vec3 pos = Vec3.atCenterOf(blockPos);
        // Sound
        for (byte note : beat.getNotes()) {
            float pitch = getPitchFromNote(note);
            level.playSeededSound(Minecraft.getInstance().player, pos.x, pos.y, pos.z, sound, SoundSource.RECORDS, 3.0F, pitch, seed);
        }
        // Particle
        if (!level.getBlockState(blockPos.above()).canOcclude()) {
            double d = (double) beat.getMinNote() / 24.0D;
            level.addParticle(ParticleTypes.NOTE, pos.x, pos.y + 0.7D, pos.z, d, 0.0D, 0.0D);
        }
    }

    private static void playBeatOnServer(ServerLevel level, BlockPos blockPos, Holder<SoundEvent> sound, long seed, Beat beat) {
        Vec3 pos = Vec3.atCenterOf(blockPos);
        // Sound
        for (byte note : beat.getNotes()) {
            float pitch = getPitchFromNote(note);
            level.playSeededSound(null, pos.x, pos.y, pos.z, sound, SoundSource.RECORDS, 3.0F, pitch, seed);
        }
        // Particle
        if (!level.getBlockState(blockPos.above()).canOcclude()) {
            double d = (double) beat.getMinNote() / 24.0D;
            level.sendParticles(ParticleTypes.NOTE, pos.x, pos.y + 0.7D, pos.z, 0, d, 0.0D, 0.0D, 1.0D);
        }
    }

    public static byte getDefaultTickPerBeat() {
        return (MIN_TICK_PER_BEAT + MAX_TICK_PER_BEAT) / 2;
    }

    /**
     * @param note must be in the range of 0~24, or use {@link NoteBlock#getPitchFromNote(int)}
     */
    public static float getPitchFromNote(byte note) {
        return PITCHES[note];
    }

    public void setNoteGridData(@Nullable NoteGridData noteGridData) {
        this.noteGridData = noteGridData;
    }

    public byte getTickPerBeat() {
        return tickPerBeat;
    }

    public void setTickPerBeat(byte tickPerBeat) {
        this.tickPerBeat = (byte) Mth.clamp(tickPerBeat, MIN_TICK_PER_BEAT, MAX_TICK_PER_BEAT);
    }

    public void handleUpdateTag(CompoundTag tag) {
        byte[] data = tag.getByteArray("PlayerData");
        setTickPerBeat(data[0]);
        tickSinceLastBeat = data[1];
        beatNumber = data[2];
        pageNumber = data[3];
    }

    public void getUpdateTag(CompoundTag tag) {
        byte[] data = new byte[4];
        data[0] = tickPerBeat;
        data[1] = tickSinceLastBeat;
        data[2] = beatNumber;
        data[3] = pageNumber;
        tag.putByteArray("PlayerData", data);
    }

    public void load(CompoundTag tag) {
        setTickPerBeat(tag.getByte("TickPerBeat"));
        tickSinceLastBeat = tag.getByte("Interval");
        beatNumber = tag.getByte("Beat");
        pageNumber = tag.getByte("Page");
    }

    public void saveAdditional(CompoundTag tag) {
        tag.putByte("TickPerBeat", tickPerBeat);
        tag.putByte("Interval", tickSinceLastBeat);
        tag.putByte("Beat", beatNumber);
        tag.putByte("Page", pageNumber);
    }

    /**
     * Called every tick when the music box is playing.
     */
    public void tick(Level level, BlockPos blockPos, BlockState blockState) {
        if (++tickSinceLastBeat >= tickPerBeat) {
            nextBeat(level, blockPos, blockState, true);
        }
    }

    /**
     * @param onClient Whether the beat will be played on the client rather than the server.
     */
    public void nextBeat(Level level, BlockPos blockPos, BlockState blockState, boolean onClient) {
        tickSinceLastBeat = 0;
        if (noteGridData == null) {
            return;
        }
        Beat lastBeat = beat;
        try {
            beat = noteGridData.getPage(pageNumber).getBeat(beatNumber, EMPTY_BEAT);
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            LISTENER.onFinish(level, blockPos, blockState);
        }
        LISTENER.onBeat(level, blockPos, blockState, lastBeat, beat);
        if (level.isClientSide == onClient) {
            playBeat(level, blockPos, blockState, beat);
        }
        if (++beatNumber >= Page.BEATS_SIZE) {
            nextPage(level, blockPos, blockState);
        }
    }

    private void nextPage(Level level, BlockPos blockPos, BlockState blockState) {
        beatNumber = 0;
        if (++pageNumber >= noteGridData.size()) {
            LISTENER.onFinish(level, blockPos, blockState);
        }
        LISTENER.onPageChange(level, blockPos, blockState, pageNumber);
    }

    public void reset() {
        noteGridData = null;
        pageNumber = 0;
        beatNumber = 0;
        tickSinceLastBeat = 0;
        beat = new Beat();
    }

    public byte getMinNote() {
        return beat.getMinNote();
    }
}
