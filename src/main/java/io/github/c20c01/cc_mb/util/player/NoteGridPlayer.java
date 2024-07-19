package io.github.c20c01.cc_mb.util.player;

import io.github.c20c01.cc_mb.block.MusicBoxBlock;
import io.github.c20c01.cc_mb.block.entity.SoundBoxBlockEntity;
import io.github.c20c01.cc_mb.data.Beat;
import io.github.c20c01.cc_mb.data.NoteGridData$;
import io.github.c20c01.cc_mb.data.Page;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
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
    private byte tickPerBeat = (MIN_TICK_PER_BEAT + MAX_TICK_PER_BEAT) / 2;
    private byte tickSinceLastBeat;
    private byte beatNumber;
    private byte pageNumber;
    private Beat beat = new Beat();
    private NoteGridData$ noteGridData = null;//TODO Non persistent data

    public NoteGridPlayer(PlayerListener listener) {
        LISTENER = listener;
    }

    /**
     * Play the current beat on the local minecraft instance.
     * SoundSeed is used to locate the specific sound from the sound event.
     */
    @OnlyIn(Dist.CLIENT)
    private static void playBeat(ClientLevel level, BlockPos blockPos, BlockState blockState, Beat beat) {
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
        double x = (double) blockPos.getX() + 0.5D;
        double y = (double) blockPos.getY() + 0.5D;
        double z = (double) blockPos.getZ() + 0.5D;
        // Sound
        for (byte note : beat.getNotes()) {
            float pitch = getPitchFromNote(note);
            level.playSeededSound(Minecraft.getInstance().player, x, y, z, soundEvent, SoundSource.RECORDS, 3.0F, pitch, soundSeed);
        }
        // Particle
        if (!level.getBlockState(blockPos.above()).canOcclude()) {
            level.addParticle(ParticleTypes.NOTE, x, y + 0.7D, z, (double) beat.getMinNote() / 24.0D, 0.0D, 0.0D);
        }
    }

    /**
     * @param note must be in the range of 0~24, or use {@link NoteBlock#getPitchFromNote(int)}
     */
    public static float getPitchFromNote(byte note) {
        return PITCHES[note];
    }

    public void setNoteGridData(@Nullable NoteGridData$ noteGridData) {
        this.noteGridData = noteGridData;
    }

    public byte getTickPerBeat() {
        return tickPerBeat;
    }

    public void setTickPerBeat(byte tickPerBeat) {
        this.tickPerBeat = (byte) Mth.clamp(tickPerBeat, MIN_TICK_PER_BEAT, MAX_TICK_PER_BEAT);
    }

    public void load(CompoundTag tag) {
        setTickPerBeat(tag.getByte("TickPerBeat"));
        tickSinceLastBeat = tag.getByte("Interval");
        beatNumber = tag.getByte("Beat");
        pageNumber = tag.getByte("Page");
        beat = Beat.ofNotes(tag.getByteArray("LastBeat"));

        // TODO
        if (tag.contains("NoteGridData")) {
            noteGridData = NoteGridData$.ofTag(tag.getCompound("NoteGridData"));
        }
    }

    public void saveAdditional(CompoundTag tag) {
        tag.putByte("TickPerBeat", tickPerBeat);
        tag.putByte("Interval", tickSinceLastBeat);
        tag.putByte("Beat", beatNumber);
        tag.putByte("Page", pageNumber);
        tag.putByteArray("LastBeat", beat.getNotes());

        // TODO
        if (noteGridData != null) {
            tag.put("NoteGridData", noteGridData.save(new CompoundTag()));
        }
    }

    /**
     * Called every tick when the music box is playing.
     */
    public void tick(Level level, BlockPos blockPos, BlockState blockState) {
        if (++tickSinceLastBeat >= tickPerBeat) {
            nextBeat(level, blockPos, blockState);
        }
    }

    public void nextBeat(Level level, BlockPos blockPos, BlockState blockState) {
        tickSinceLastBeat = 0;
        if (noteGridData == null) {
            return;
        }
        Beat lastBeat = beat;
        beat = noteGridData.getPage(pageNumber).getBeat(beatNumber, EMPTY_BEAT);
        LISTENER.onBeat(level, blockPos, blockState, lastBeat, beat);
        if (level.isClientSide) {
            playBeat((ClientLevel) level, blockPos, blockState, beat);
        }
        if (++beatNumber >= Page.BEATS_SIZE) {
            nextPage(level, blockPos, blockState);
        }
    }

    private void nextPage(Level level, BlockPos blockPos, BlockState blockState) {
        beatNumber = 0;
        if (++pageNumber >= noteGridData.size()) {
            LISTENER.onFinish(level, blockPos, blockState);
            // reset();
        }
    }

    public void reset() {
        noteGridData = null;
        pageNumber = 0;
        beatNumber = 0;
        tickSinceLastBeat = 0;
    }

    public byte getMinNote() {
        return beat.getMinNote();
    }
}
