package io.github.c20c01.cc_mb.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public class SoundPlayer {
    public static void playInMind(SoundEvent event, long seed, float volume, float pitch) {
        MusicBoxSoundInstance soundInstance = new MusicBoxSoundInstance(event.location(), seed, volume, pitch, 0.0D, 0.0D, 0.0D, true);
        Minecraft.getInstance().getSoundManager().play(soundInstance);
    }

    public static void playInLevel(SoundEvent event, long seed, float volume, float pitch, Vec3 pos) {
        MusicBoxSoundInstance soundInstance = new MusicBoxSoundInstance(event.location(), seed, volume, pitch, pos.x, pos.y, pos.z, false);
        Minecraft.getInstance().getSoundManager().play(soundInstance);
    }

    public static class MusicBoxSoundInstance extends AbstractSoundInstance {
        public MusicBoxSoundInstance(ResourceLocation soundLocation, long seed, float volume, float pitch, double x, double y, double z, boolean relative) {
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
