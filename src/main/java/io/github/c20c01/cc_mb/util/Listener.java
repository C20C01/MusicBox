package io.github.c20c01.cc_mb.util;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundSourceEvent;
import net.neoforged.neoforge.common.NeoForge;

import javax.annotation.Nullable;

/**
 * Listen to the sound event and get the last sound event location.
 */
@OnlyIn(Dist.CLIENT)
public class Listener {
    private static final Listener LISTENER = new Listener();
    private boolean heard = false;
    private ResourceLocation soundLocation = null;

    public static void start() {
        NeoForge.EVENT_BUS.register(LISTENER);
    }

    @Nullable
    public static ResourceLocation getLocation() {
        if (LISTENER.heard) {
            LISTENER.heard = false;
            return LISTENER.soundLocation;
        } else {
            return null;
        }
    }

    @Nullable
    public static ResourceLocation getFinalResult() {
        NeoForge.EVENT_BUS.unregister(LISTENER);
        ResourceLocation result = LISTENER.soundLocation;
        LISTENER.soundLocation = null;
        return result;
    }

    @SubscribeEvent
    public void listen(PlaySoundSourceEvent event) {
        heard = true;
        soundLocation = event.getSound().getLocation();
    }
}