package io.github.c20c01.cc_mb.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.client.Listener;
import io.github.c20c01.cc_mb.network.SoundShardUpdatePacket;
import io.github.c20c01.cc_mb.util.MobListenAndActHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Consumer;

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
                enchantment -> player.getCooldowns().addCooldown(soundShard, DEFAULT_COOL_DOWN - 10 * Mth.clamp(soundShard.getEnchantmentLevel(enchantment), 0, 5)),
                () -> player.getCooldowns().addCooldown(soundShard, DEFAULT_COOL_DOWN)
        );
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack soundShard = player.getItemInHand(hand);
        addCooldown(level, player, soundShard);
        Optional<SoundInfo> info = SoundInfo.ofItemStack(soundShard);
        if (info.isEmpty() || info.get().soundEvent == null) {
            // start listening to the sound event
            if (level.isClientSide) {
                Listener.start();
            }
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }
        if (player.getAbilities().instabuild) {
            if (player.isSecondaryUseActive()) {
                // creative mode only: shift to change the sound seed.
                Long newSeed = tryToChangeSoundSeed(soundShard, level.random);
                if (newSeed != null) {
                    level.playSeededSound(player, player, info.get().soundEvent, player.getSoundSource(), 1.0F, 1.0F, newSeed);
                }
                return InteractionResult.SUCCESS_SERVER;
            }
            if (hand == InteractionHand.OFF_HAND) {
                // creative mode only: off-hand to reset the sound shard.
                soundShard.remove(CCMain.SOUND_INFO.get());
                return InteractionResult.SUCCESS_SERVER;
            }
        }
        var soundEvent = info.get().soundEvent;
        // play the sound event that saved in the sound shard
        level.playSeededSound(player, player, soundEvent, player.getSoundSource(), 1.0F, 1.0F, info.get().soundSeed.orElseGet(level.random::nextLong));
        if (!level.isClientSide) {
            MobListenAndActHelper.nearbyMobsListen(level, player.blockPosition(), soundEvent.value().location());
        }
        level.gameEvent(player, GameEvent.INSTRUMENT_PLAY, player.position());
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack itemStack, int tick) {
        if (level.isClientSide && livingEntity instanceof Player player) {
            ResourceLocation location = Listener.getLocation();
            if (location != null) {
                // Display the sound event that the player is listening to.
                player.displayClientMessage(Listener.getSoundEventTitle(location).withStyle(ChatFormatting.GOLD), true);
            }
        }
        super.onUseTick(level, livingEntity, itemStack, tick);
    }

    @Override
    public boolean releaseUsing(ItemStack itemStack, Level level, LivingEntity livingEntity, int tick) {
        if (level.isClientSide && livingEntity instanceof Player player) {
            ResourceLocation location = Listener.finish();
            if (location != null) {
                player.displayClientMessage(Listener.getSoundEventTitle(location).withStyle(ChatFormatting.DARK_GREEN), true);
                // Send the sound event to the server to save it in the sound shard.
                ClientPacketDistributor.sendToServer(new SoundShardUpdatePacket(player.getInventory().getSelectedSlot(), location));
                return true;
            }
        }
        return super.releaseUsing(itemStack, level, livingEntity, tick);
    }

    @Override
    public void onStopUsing(ItemStack stack, LivingEntity entity, int count) {
        if (entity.level().isClientSide) {
            // make sure the listener is removed.
            Listener.finish();
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, flag);
        SoundInfo.ofItemStack(stack).ifPresent(info -> info.addToTooltip(context, tooltipAdder, flag, stack));
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
    public ItemUseAnimation getUseAnimation(ItemStack itemStack) {
        return ItemUseAnimation.BOW;
    }

    /**
     * Reset the sound shard with the sound event and the sound seed.
     */
    private static class ResetSoundShard implements CauldronInteraction {
        @Override
        public InteractionResult interact(BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand hand, ItemStack itemStack) {
            if (SoundShard.SoundInfo.ofItemStack(itemStack).isPresent() && !level.isClientSide()) {
                itemStack.remove(CCMain.SOUND_INFO.get());
                LayeredCauldronBlock.lowerFillLevel(blockState, level, blockPos);
                level.playSound(null, blockPos, SoundEvents.POWDER_SNOW_FALL, SoundSource.BLOCKS, 1.0F, 1.0F);
                return InteractionResult.SUCCESS_SERVER;
            }
            return InteractionResult.FAIL;
        }
    }

    public record SoundInfo(Holder<SoundEvent> soundEvent, Optional<Long> soundSeed) implements TooltipProvider {
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
                return soundSeed.equals(seed) && soundEvent.value().location().equals(event.value().location());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return 31 * soundEvent.value().location().hashCode() + soundSeed.hashCode();
        }

        public static Optional<SoundInfo> ofItemStack(ItemStack soundShard) {
            return Optional.ofNullable(soundShard.get(CCMain.SOUND_INFO.get()));
        }

        @Override
        public String toString() {
            return "SoundInfo{soundEvent=%s, soundSeed=%s}".formatted(soundEvent, soundSeed);
        }

        @Override
        public void addToTooltip(TooltipContext context, Consumer<Component> tooltipAdder, TooltipFlag flag, DataComponentGetter componentGetter) {
            ChatFormatting color = soundSeed.isPresent() ? ChatFormatting.DARK_GREEN : ChatFormatting.GOLD;
            // Display the sound event that saved in the sound shard.
            // Green for the fixed seed, yellow for the random seed.
            tooltipAdder.accept(Listener.getSoundEventTitle(soundEvent.value().location()).withStyle(color));
        }
    }
}