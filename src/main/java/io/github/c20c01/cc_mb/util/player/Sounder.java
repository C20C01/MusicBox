package io.github.c20c01.cc_mb.util.player;

import io.github.c20c01.cc_mb.client.SoundPlayer;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.phys.Vec3;

public class Sounder {
    public static final byte MAX_OCTAVE = 2;
    public static final byte MIN_OCTAVE = -2;
    private static final float[] PITCHES = new float[25];

    static {
        for (int i = 0; i < PITCHES.length; i++) {
            PITCHES[i] = NoteBlock.getPitchFromNote(i);
        }
    }

    public long seed;
    public Holder<SoundEvent> sound;
    public float volume = 3f;
    private byte octave;
    private float pitchFactor = 1f;

    /**
     * @param note must be in the range of 0~24, or use {@link NoteBlock#getPitchFromNote(int)}
     */
    public static float getPitchFromNote(byte note) {
        return PITCHES[note];
    }

    protected void playInMind(byte note) {
        SoundPlayer.playInMind(sound.value(), seed, volume, getPitchFromNote(note));
    }

    protected void playInLevel(byte note, Vec3 pos) {
        SoundPlayer.playInLevel(sound.value(), seed, volume, getOctaveFixedPitch(note), pos);
    }

    public float getOctaveFixedPitch(byte note) {
        return pitchFactor * getPitchFromNote(note);
    }

    public byte getOctave() {
        return octave;
    }

    public void setOctave(byte octave) {
        this.octave = (byte) Mth.clamp(octave, MIN_OCTAVE, MAX_OCTAVE);
        this.pitchFactor = (float) Math.pow(2.0, octave);
    }
}
