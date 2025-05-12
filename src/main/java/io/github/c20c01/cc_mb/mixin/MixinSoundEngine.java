package io.github.c20c01.cc_mb.mixin;

import io.github.c20c01.cc_mb.client.SoundPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public class MixinSoundEngine {
    /**
     * @author C20C01 @ GitHub
     * @reason Skip the pitch calculation for {@link SoundPlayer.MusicBoxSoundInstance} to expand the pitch range.
     */
    @Inject(method = "calculatePitch", at = @At("HEAD"), cancellable = true)
    private void expandPitch(SoundInstance sound, CallbackInfoReturnable<Float> cir) {
        if (sound instanceof SoundPlayer.MusicBoxSoundInstance) {
            cir.setReturnValue(sound.getPitch());
            cir.cancel();
        }
    }
}
