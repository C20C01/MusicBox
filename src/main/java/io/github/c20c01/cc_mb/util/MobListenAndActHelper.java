package io.github.c20c01.cc_mb.util;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * Make mob act according to the sound nearby.
 * <p>
 * For now, it can tempt the mob based on the sound which from the same mob type
 * (e.g. cow and mooshroom will be both tempted by the "minecraft:entity.cow.ambient").
 * <p>
 * Use {@link SoundBehaviors#BEHAVIOR_INITIALIZERS} to add more behaviors if needed.
 */
public class MobListenAndActHelper {
    public static final Map<String, SoundBehaviors> BEHAVIORS_CACHE = new HashMap<>();

    public static void nearbyMobsListen(Level level, BlockPos source, ResourceLocation soundLocation) {
        List<PathfinderMob> mobs = level.getEntities(
                EntityTypeTest.forClass(PathfinderMob.class),
                new AABB(source).inflate(8),
                mob -> {
                    if (!mob.isAlive()) return false;
                    return !(mob instanceof TamableAnimal tamableAnimal) || !tamableAnimal.isInSittingPose();
                }
        );
        for (Mob mob : mobs) {
            listen(level, source, mob, soundLocation);
        }
    }

    public static void listen(Level level, BlockPos source, Mob mob, ResourceLocation soundLocation) {
        String className = mob.getClass().getName();
        SoundBehaviors behaviors = BEHAVIORS_CACHE.get(className);
        if (behaviors == null) {
            behaviors = SoundBehaviors.init(mob);
        }
        behaviors.get(soundLocation).act(level, source, mob);
    }

    public interface SoundBehavior {
        SoundBehavior TEMPT = (level, source, mob) -> {
            Vec3 to = Vec3.atCenterOf(source);
            mob.getNavigation().moveTo(to.x, to.y, to.z, mob.getMoveControl().getSpeedModifier());
        };
        SoundBehavior EMPTY = (level, source, mob) -> {
        };

        void act(Level level, BlockPos source, Mob mob);
    }

    public static class SoundBehaviors {
        public static final Collection<BiConsumer<Mob, SoundBehaviors>> BEHAVIOR_INITIALIZERS = List.of(
                AmbientBehaviorInitializer::init
        );

        private final Map<ResourceLocation, SoundBehavior> map = new HashMap<>();

        public static SoundBehaviors init(Mob mob) {
            SoundBehaviors instance = new SoundBehaviors();
            for (var initializer : BEHAVIOR_INITIALIZERS) {
                initializer.accept(mob, instance);
            }
            return instance;
        }

        public SoundBehavior get(ResourceLocation soundLocation) {
            return map.getOrDefault(soundLocation, SoundBehavior.EMPTY);
        }
    }

    /**
     * Initializes the ambient behaviors for a given Mob.
     * <p>
     * Use ASM to collect all static fields of type `SoundEvent` in {@link Mob#getAmbientSound()},
     * so it may collect the wrong sound (won't happen in vanilla).
     * <p>
     * Use Reflection to get the actual `SoundEvent` instances from the fields.
     */
    private static class AmbientBehaviorInitializer {
        private static MobSoundsVisitor visitMobClass(String className) {
            ClassReader classReader;
            try {
                classReader = new ClassReader(className);
            } catch (IOException e) {
                LogUtils.getLogger().error("Failed to read class for mob {}", className, e);
                return null;
            }
            MobSoundsVisitor mobClassVisitor = new MobSoundsVisitor();
            classReader.accept(mobClassVisitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return mobClassVisitor;
        }

        private static void init(Mob mob, SoundBehaviors instance) {
            String className = mob.getClass().getName();
            List<String> visitedClassNames = new ArrayList<>();
            while (true) {
                visitedClassNames.add(className);

                MobSoundsVisitor cv;
                if ((cv = visitMobClass(className)) == null) {
                    for (String visited : visitedClassNames) {
                        BEHAVIORS_CACHE.put(visited, instance);
                    }
                    break;
                }

                if (cv.methodFound) {
                    for (SoundEvent soundEvent : cv.AmbientSounds) {
                        instance.map.put(soundEvent.getLocation(), SoundBehavior.TEMPT);
                    }
                    for (String visited : visitedClassNames) {
                        BEHAVIORS_CACHE.put(visited, instance);
                    }
                    break;
                }

                className = cv.superName;
                if (className == null || className.equals("net/minecraft/world/entity/LivingEntity")) {
                    for (String visited : visitedClassNames) {
                        BEHAVIORS_CACHE.put(visited, instance);
                    }
                    break;
                }

                SoundBehaviors cachedBehaviors;
                if ((cachedBehaviors = BEHAVIORS_CACHE.get(className)) != null) {
                    for (String visited : visitedClassNames) {
                        BEHAVIORS_CACHE.put(visited, cachedBehaviors);
                    }
                    break;
                }
            }
            LogUtils.getLogger().debug("{} ambient: {}", mob.getClass().getName(), instance.map.keySet());
        }

        private static class MobSoundsVisitor extends ClassVisitor {
            public Set<SoundEvent> AmbientSounds = new HashSet<>();
            public boolean methodFound = false;
            public String superName = null;

            public MobSoundsVisitor() {
                super(Opcodes.ASM9);
            }

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                this.superName = superName;
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                if (name.equals("getAmbientSound")) {
                    methodFound = true;
                    return new StaticSoundFieldVisitor(AmbientSounds);
                }
                return null;
            }
        }

        private static class StaticSoundFieldVisitor extends MethodVisitor {
            private final Set<SoundEvent> soundEvents;

            public StaticSoundFieldVisitor(Set<SoundEvent> soundEvents) {
                super(Opcodes.ASM9);
                this.soundEvents = soundEvents;
            }

            private static SoundEvent getSoundEvent(String owner, String name) {
                try {
                    Class<?> soundClass = Class.forName(owner.replace('/', '.'));
                    Field field = soundClass.getDeclaredField(name);
                    field.setAccessible(true);
                    return (SoundEvent) field.get(null);
                } catch (NoSuchFieldException e) {
                    LogUtils.getLogger().warn("Field {} not found in class {}", name, owner);
                } catch (ClassNotFoundException e) {
                    LogUtils.getLogger().warn("Class {} not found", owner);
                } catch (IllegalAccessException e) {
                    LogUtils.getLogger().warn("Field {} in class {} is not accessible", name, owner);
                } catch (ClassCastException e) {
                    LogUtils.getLogger().warn("Field {} in class {} is not a SoundEvent", name, owner);
                }
                return null;
            }

            @Override
            public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                if (opcode == Opcodes.GETSTATIC && descriptor.equals("Lnet/minecraft/sounds/SoundEvent;")) {
                    SoundEvent soundEvent = getSoundEvent(owner, name);
                    if (soundEvent != null) {
                        soundEvents.add(soundEvent);
                    }
                }
            }
        }
    }
}
