package io.github.c20c01.cc_mb.util;

import io.github.c20c01.cc_mb.mixin.MixinMob;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Make mob act according to the sound nearby.
 * <p>
 * For now, it can tempt the mob based on the sound which from the same mob type
 * (e.g. cow and mooshroom will be both tempted by the "minecraft:entity.cow.ambient").
 * <p>
 * Use {@link #LISTENERS} to add more behaviors if needed.
 */
public class MobListenAndActHelper {
    public static final List<Listener> LISTENERS = new ArrayList<>();

    static {
        LISTENERS.add(Listener.AMBIENT_SOUND);
    }

    public static void nearbyMobsListen(Level level, BlockPos source, ResourceLocation soundLocation) {
        List<PathfinderMob> mobs = level.getEntities(
                EntityTypeTest.forClass(PathfinderMob.class),
                new AABB(source).inflate(8),
                LivingEntity::isAlive
        );
        for (Mob mob : mobs) {
            listen(level, source, mob, soundLocation);
        }
    }

    public static void listen(Level level, BlockPos source, Mob mob, ResourceLocation soundLocation) {
        for (Listener listener : LISTENERS) {
            if (listener.listen(level, source, mob, soundLocation)) return;
        }
    }

    public interface Listener {
        Listener AMBIENT_SOUND = (level, source, mob, soundLocation) -> {
            PathNavigation navigation = mob.getNavigation();
            if (navigation.isInProgress()) {
                return false;
            }

            if (mob instanceof TamableAnimal tamableAnimal && tamableAnimal.isInSittingPose()) {
                return false;
            }

            ResourceLocation ambientSound = ((MixinMob) mob).invokeGetAmbientSound().getLocation();
            if (soundLocation.equals(ambientSound)) {
                Vec3 to = Vec3.atCenterOf(source);
                navigation.moveTo(to.x, to.y, to.z, mob.getMoveControl().getSpeedModifier());
                return true;
            }

            return false;
        };

        boolean listen(Level level, BlockPos source, Mob mob, ResourceLocation soundLocation);
    }
}
