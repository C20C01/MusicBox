package io.github.c20c01.cc_mb.player;

import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;

import javax.annotation.Nullable;

public class SpeakerConfig {
    private boolean hasSpecificSeed = false;
    private long seed;
    private byte octave = 0;
    private float pitchFactor = 1f;

    @Nullable
    private Identifier soundLocation = null;

    public void setNullableSeed(@Nullable Long seed) {
        if (seed == null) removeSeed();
        else setSeed(seed);
    }

    public void removeSeed() {
        this.hasSpecificSeed = false;
    }

    public boolean hasSpecificSeed() {
        return hasSpecificSeed;
    }

    /**
     * @return the specific seed, check {@link #hasSpecificSeed()} before using this method
     */
    public long getSpecificSeed() {
        return seed;
    }

    public long getSeed(RandomSource random) {
        return hasSpecificSeed ? seed : random.nextLong();
    }

    @Nullable
    public Long getSeed() {
        return hasSpecificSeed ? seed : null;
    }

    public void setSeed(long seed) {
        this.seed = seed;
        this.hasSpecificSeed = true;
    }

    public byte getOctave() {
        return octave;
    }

    public void setOctave(byte octave) {
        this.octave = Octave.clamp(octave);
        this.pitchFactor = (float) Math.pow(2.0, octave);
    }

    public float getPitchFactor() {
        return pitchFactor;
    }

    public void removeSoundLocation() {
        this.soundLocation = null;
    }

    @Nullable
    public Identifier getSoundLocation() {
        return soundLocation;
    }

    public void setSoundLocation(@Nullable Identifier soundLocation) {
        this.soundLocation = soundLocation;
    }

    public boolean isSilent() {
        return soundLocation == null;
    }
}
