package io.github.c20c01.cc_mb.player;

import io.github.c20c01.cc_mb.MusicBox;
import io.github.c20c01.cc_mb.item.SoundShard;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class SpeakerConfig {
    public static final SpeakerConfig MIND_PLAYER_DEFAULT = new SpeakerConfig(SoundEvents.NOTE_BLOCK_HARP.value().location(), 0L);

    @Nullable
    private Identifier soundLocation;
    private boolean hasSpecificSeed = false;
    private long seed;
    private byte octave = 0;
    private float pitchFactor = 1f;

    public SpeakerConfig() {
        this.soundLocation = null;
    }

    public SpeakerConfig(@Nullable Identifier soundLocation, @Nullable Long seed) {
        this.soundLocation = soundLocation;
        if (seed != null) this.setSeed(seed);
    }

    @Nullable
    public static SpeakerConfig ofItemStack(ItemStack itemStack) {
        if (itemStack.isEmpty()) return null;
        if (!itemStack.is(MusicBox.SOUND_SHARD_ITEM.get())) return null;

        SoundShard.SoundInfo soundInfo = SoundShard.SoundInfo.ofItemStack(itemStack).orElse(null);
        if (soundInfo == null) return null;

        return new SpeakerConfig(soundInfo.soundEvent().value().location(), soundInfo.soundSeed().orElse(null));
    }

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
