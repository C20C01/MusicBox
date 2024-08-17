package io.github.c20c01.cc_mb.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Listen to the sound event and get the last sound event location.
 */
@Environment(EnvType.CLIENT)
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
        LISTENER.listening = false;
        return LISTENER.soundLocation;
    }

    @Override
    public void onPlaySound(@NotNull SoundInstance soundInstance, @NotNull WeighedSoundEvents accessor) {
        shown = false;
        soundLocation = soundInstance.getLocation();
    }
}