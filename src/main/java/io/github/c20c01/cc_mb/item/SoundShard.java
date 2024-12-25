package io.github.c20c01.cc_mb.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.network.SoundShardUpdatePacket;
import io.github.c20c01.cc_mb.util.Listener;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class SoundShard extends Item {
    private static final int DEFAULT_COOL_DOWN = 55;

    public SoundShard(Properties properties) {
        super(properties);
        CauldronInteraction.POWDER_SNOW.map().put(this, new ResetSoundShard());
    }

    /**
     * @return True if the item has a sound event. (The sound seed is not necessary)
     */
    public static boolean containSound(ItemStack itemStack) {
        return itemStack.get(CCMain.SOUND_INFO.get()) != null;
    }

    @OnlyIn(Dist.CLIENT)
    private static MutableComponent getSoundEventTitle(ResourceLocation location) {
        var sound = Minecraft.getInstance().getSoundManager().getSoundEvent(location);
        if (sound != null && sound.getSubtitle() != null) {
            return MutableComponent.create(sound.getSubtitle().getContents());
        }
        return Component.literal("? ? ?");
    }

    /**
     * Change the sound seed of the sound shard.
     *
     * @return The new sound seed or null.
     */
    @Nullable
    public static Long tryToChangeSoundSeed(ItemStack soundShard, RandomSource random) {
        Optional<SoundInfo> info = SoundInfo.ofItemStack(soundShard);
        if (info.isPresent()) {
            long newSeed = random.nextLong();
            soundShard.set(CCMain.SOUND_INFO.get(), new SoundInfo(info.get().soundEvent(), Optional.of(newSeed)));
            return newSeed;
        }
        return null;
    }

    /**
     * Add the cooldown to the player after using the sound shard.
     * <p>
     * Efficiency level will affect the cooldown time.
     */
    private void addCooldown(Level level, Player player, ItemStack soundShard) {
        level.registryAccess().lookup(Registries.ENCHANTMENT).flatMap(registry -> registry.get(Enchantments.EFFICIENCY)).ifPresentOrElse(
                enchantment -> player.getCooldowns().addCooldown(this, DEFAULT_COOL_DOWN - 10 * Mth.clamp(soundShard.getEnchantmentLevel(enchantment), 0, 5)),
                () -> player.getCooldowns().addCooldown(this, DEFAULT_COOL_DOWN)
        );
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack soundShard = player.getItemInHand(hand);
        addCooldown(level, player, soundShard);
        Optional<SoundInfo> info = SoundInfo.ofItemStack(soundShard);
        if (info.isEmpty() || info.get().soundEvent == null) {
            // start listening to the sound event
            if (level.isClientSide) {
                Listener.start();
            }
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(soundShard);
        }
        if (player.getAbilities().instabuild) {
            if (player.isSecondaryUseActive()) {
                // creative mode only: shift to change the sound seed.
                Long newSeed = tryToChangeSoundSeed(soundShard, level.random);
                if (newSeed != null) {
                    level.playSeededSound(null, player.getX(), player.getY(), player.getZ(), info.get().soundEvent.value(), player.getSoundSource(), 1.0F, 1.0F, newSeed);
                }
                return InteractionResultHolder.sidedSuccess(soundShard, level.isClientSide);
            }
            if (hand == InteractionHand.OFF_HAND) {
                // creative mode only: off-hand to reset the sound shard.
                soundShard.remove(CCMain.SOUND_INFO.get());
                return InteractionResultHolder.sidedSuccess(soundShard, level.isClientSide);
            }
        }
        // play the sound event that saved in the sound shard
        level.playSeededSound(null, player.getX(), player.getY(), player.getZ(), info.get().soundEvent(), player.getSoundSource(), 1.0F, 1.0F, info.get().soundSeed.orElseGet(level.random::nextLong));
        return InteractionResultHolder.sidedSuccess(soundShard, level.isClientSide);
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack itemStack, int tick) {
        if (level.isClientSide && livingEntity instanceof Player player) {
            ResourceLocation location = Listener.getLocation();
            if (location != null) {
                // Display the sound event that the player is listening to.
                player.displayClientMessage(getSoundEventTitle(location).withStyle(ChatFormatting.GOLD), true);
            }
        }
        super.onUseTick(level, livingEntity, itemStack, tick);
    }

    @Override
    public void releaseUsing(ItemStack itemStack, Level level, LivingEntity livingEntity, int tick) {
        if (level.isClientSide && livingEntity instanceof Player player) {
            ResourceLocation location = Listener.finish();
            if (location != null) {
                player.displayClientMessage(getSoundEventTitle(location).withStyle(ChatFormatting.DARK_GREEN), true);
                // Send the sound event to the server to save it in the sound shard.
                PacketDistributor.sendToServer(new SoundShardUpdatePacket(player.getInventory().selected, location));
            }
        }
        super.releaseUsing(itemStack, level, livingEntity, tick);
    }

    @Override
    public void onStopUsing(ItemStack stack, LivingEntity entity, int count) {
        if (entity.level().isClientSide) {
            // make sure the listener is removed.
            Listener.finish();
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        SoundInfo.ofItemStack(stack).ifPresent(info -> {
            ChatFormatting color = info.soundSeed.isPresent() ? ChatFormatting.DARK_GREEN : ChatFormatting.GOLD;
            // Display the sound event that saved in the sound shard.
            // Green for the fixed seed, yellow for the random seed.
            tooltipComponents.add(getSoundEventTitle(info.soundEvent.value().getLocation()).withStyle(color));
        });
    }

    @Override
    public boolean isFoil(ItemStack itemStack) {
        return containSound(itemStack);
    }

    @Override
    public int getUseDuration(ItemStack itemStack, LivingEntity entity) {
        return 600;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack itemStack) {
        return UseAnim.BOW;
    }

    /**
     * Reset the sound shard with the sound event and the sound seed.
     */
    private static class ResetSoundShard implements CauldronInteraction {
        @Override
        public ItemInteractionResult interact(BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand hand, ItemStack itemStack) {
            if (SoundShard.SoundInfo.ofItemStack(itemStack).isPresent()) {
                itemStack.remove(CCMain.SOUND_INFO.get());
                LayeredCauldronBlock.lowerFillLevel(blockState, level, blockPos);
                level.playSound(null, blockPos, SoundEvents.POWDER_SNOW_FALL, SoundSource.BLOCKS, 1.0F, 1.0F);
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
            return ItemInteractionResult.FAIL;
        }
    }

    public record SoundInfo(Holder<SoundEvent> soundEvent, Optional<Long> soundSeed) {
        public static final Codec<SoundInfo> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        SoundEvent.CODEC.fieldOf("sound_event").forGetter(SoundInfo::soundEvent),
                        Codec.LONG.optionalFieldOf("sound_seed").forGetter(SoundInfo::soundSeed)
                ).apply(instance, SoundInfo::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, SoundInfo> STREAM_CODEC = StreamCodec.composite(
                SoundEvent.STREAM_CODEC,
                SoundInfo::soundEvent,
                ByteBufCodecs.optional(ByteBufCodecs.VAR_LONG),
                SoundInfo::soundSeed,
                SoundInfo::new
        );

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof SoundInfo(Holder<SoundEvent> event, Optional<Long> seed)) {
                return soundSeed.equals(seed) && soundEvent.value().getLocation().equals(event.value().getLocation());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return 31 * soundEvent.value().getLocation().hashCode() + soundSeed.hashCode();
        }

        public static Optional<SoundInfo> ofItemStack(ItemStack soundShard) {
            return Optional.ofNullable(soundShard.get(CCMain.SOUND_INFO.get()));
        }
    }
}