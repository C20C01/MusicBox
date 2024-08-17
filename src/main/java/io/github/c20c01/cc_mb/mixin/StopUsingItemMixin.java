package io.github.c20c01.cc_mb.mixin;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.util.Listener;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Make sure closing the listener when the player stops using the sound shard,
 * release using is not enough.
 */
@Mixin(LocalPlayer.class)
public abstract class StopUsingItemMixin {
    @Inject(at = @At("HEAD"), method = "stopUsingItem")
    private void stopUsingItem(CallbackInfo info) {
        if (((LocalPlayer) (Object) this).getUseItem().is(CCMain.SOUND_SHARD_ITEM)) {
            Listener.finish();
        }
    }
}
