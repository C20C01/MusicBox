package io.github.c20c01.cc_mb.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/**
 * Listen to the sound event and get the last sound event location.
 */
public class Listener implements SoundEventListener {
    private static final Listener LISTENER = new Listener();
    private volatile boolean isListening = false;
    private volatile Identifier soundLocation = null;
    private volatile boolean popped = true;

    public static void start() {
        LISTENER.doStart();
    }

    public static Identifier pop() {
        return LISTENER.doPop();
    }

    public static Identifier finish() {
        return LISTENER.doFinish();
    }

    public static MutableComponent getSoundEventTitle(Identifier location) {
        WeighedSoundEvents sound = Minecraft.getInstance().getSoundManager().getSoundEvent(location);
        if (sound != null && sound.getSubtitle() != null) {
            return MutableComponent.create(sound.getSubtitle().getContents());
        }
        return Component.literal("? ? ?");
    }

    private static boolean isAudible(SoundInstance sound, float range) {
        if (Float.isInfinite(range)) {
            return true;
        } else {
            Vec3 listenerPos = Minecraft.getInstance().getSoundManager().getListenerTransform().position();
            return listenerPos.distanceToSqr(sound.getX(), sound.getY(), sound.getZ()) <= (double) (range * range);
        }
    }

    private void doStart() {
        soundLocation = null;
        popped = true;
        if (!isListening) {
            Minecraft.getInstance().getSoundManager().addListener(this);
            isListening = true;
        }
    }

    @Nullable
    private Identifier doPop() {
        if (popped) {
            return null;
        } else {
            popped = true;
            return soundLocation;
        }
    }

    @Nullable
    private Identifier doFinish() {
        Minecraft.getInstance().getSoundManager().removeListener(this);
        Identifier lastSound = this.soundLocation;
        isListening = false;
        soundLocation = null;
        popped = true;
        return lastSound;
    }

    private void push(Identifier location) {
        soundLocation = location;
        popped = false;
    }

    @Override
    public void onPlaySound(@NotNull SoundInstance soundInstance, @NotNull WeighedSoundEvents accessor, float range) {
        if (isAudible(soundInstance, range)) push(soundInstance.getIdentifier());
    }
}