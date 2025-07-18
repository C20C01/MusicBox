package io.github.c20c01.cc_mb.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/**
 * Listen to the sound event and get the last sound event location.
 */
public class Listener implements SoundEventListener {
    private static final Listener LISTENER = new Listener();
    private boolean listening = false;
    private boolean shown = false;
    private ResourceLocation soundLocation = null;

    public static void start() {
        LISTENER.soundLocation = null;
        if (!LISTENER.listening) {
            LISTENER.listening = true;
            Minecraft.getInstance().getSoundManager().addListener(LISTENER);
        }
    }

    @Nullable
    public static ResourceLocation getLocation() {
        if (LISTENER.shown) {
            return null;
        } else {
            LISTENER.shown = true;
            return LISTENER.soundLocation;
        }
    }

    @Nullable
    public static ResourceLocation finish() {
        Minecraft.getInstance().getSoundManager().removeListener(LISTENER);
        if (LISTENER.listening) {
            LISTENER.listening = false;
            return LISTENER.soundLocation;
        }
        return null;
    }

    private boolean isAudible(SoundInstance sound, float range) {
        if (Float.isInfinite(range)) {
            return true;
        } else {
            Vec3 listenerPos = Minecraft.getInstance().getSoundManager().getListenerTransform().position();
            return listenerPos.distanceToSqr(sound.getX(), sound.getY(), sound.getZ()) <= (double) (range * range);
        }
    }

    public static MutableComponent getSoundEventTitle(ResourceLocation location) {
        var sound = Minecraft.getInstance().getSoundManager().getSoundEvent(location);
        if (sound != null && sound.getSubtitle() != null) {
            return MutableComponent.create(sound.getSubtitle().getContents());
        }
        return Component.literal("? ? ?");
    }

    @Override
    public void onPlaySound(@NotNull SoundInstance soundInstance, @NotNull WeighedSoundEvents accessor, float range) {
        if (isAudible(soundInstance, range)) {
            shown = false;
            soundLocation = soundInstance.getLocation();
        }
    }
}