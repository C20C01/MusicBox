package io.github.c20c01.cc_mb.mixin;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Mob.class)
public interface MixinMob {
    @Invoker("getAmbientSound")
    SoundEvent invokeGetAmbientSound();
}
