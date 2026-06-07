package io.github.c20c01.cc_mb.client;

import io.github.c20c01.cc_mb.data.Beat;
import io.github.c20c01.cc_mb.player.SpeakerConfig;
import it.unimi.dsi.fastutil.bytes.ByteArraySet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.phys.Vec3;

public class Speaker {
    private static final RandomSource RANDOM = RandomSource.create();
    private static final float[] PITCHES = new float[25];

    static {
        for (int i = 0; i < PITCHES.length; i++) {
            PITCHES[i] = NoteBlock.getPitchFromNote(i);
        }
    }

    public static void playMind(SpeakerConfig config, ByteArraySet notes) {
        Identifier soundLocation = config.getSoundLocation();
        long seed = config.getSeed(RANDOM);
        SoundManager soundManager = Minecraft.getInstance().getSoundManager();
        for (byte note : notes) {
            soundManager.play(new MusicBoxSoundInstance(soundLocation, seed, 3.0F, PITCHES[note], 0.0D, 0.0D, 0.0D, true));
        }
    }

    public static void playBox(SpeakerConfig config, Beat beat, Level level, BlockPos blockPos) {
        Vec3 pos = Vec3.atCenterOf(blockPos);
        // Sound
        Identifier soundLocation = config.getSoundLocation();
        long seed = config.getSeed(RANDOM);
        float pitchFactor = config.getPitchFactor();
        SoundManager soundManager = Minecraft.getInstance().getSoundManager();
        for (byte note : beat.getNotes()) {
            soundManager.play(new MusicBoxSoundInstance(soundLocation, seed, 3.0F, PITCHES[note] * pitchFactor, pos.x, pos.y, pos.z, false));
        }
        // Particle
        double d = (double) beat.getMinNote() / 24.0D;
        level.addParticle(ParticleTypes.NOTE, pos.x, pos.y + 0.7D, pos.z, d, 0.0D, 0.0D);
    }

    public static class MusicBoxSoundInstance extends AbstractSoundInstance {
        public MusicBoxSoundInstance(Identifier soundLocation, long seed, float volume, float pitch, double x, double y, double z, boolean relative) {
            super(soundLocation, SoundSource.RECORDS, RandomSource.create(seed));
            this.volume = volume;
            this.pitch = pitch;
            this.x = x;
            this.y = y;
            this.z = z;
            this.relative = relative;
            this.looping = false;
            this.delay = 0;
            this.attenuation = Attenuation.LINEAR;
        }
    }
}
