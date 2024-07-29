package io.github.c20c01.cc_mb.item;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.network.CCNetwork;
import io.github.c20c01.cc_mb.network.SoundShardPacket;
import io.github.c20c01.cc_mb.util.Listener;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public class SoundShard extends Item {
    public static final String SOUND_EVENT = "SoundEvent";
    public static final String SOUND_SEED = "SoundSeed";
    private static final int DEFAULT_COOL_DOWN = 55;

    public SoundShard(Properties properties) {
        super(properties);
        CauldronInteraction.POWDER_SNOW.put(this, new ResetSoundShard());
    }

    /**
     * @return True if the item has a sound event. (The sound seed is not necessary)
     */
    public static boolean containSound(ItemStack itemStack) {
        return containSound(itemStack.getTag());
    }

    /**
     * @return True if the item has a sound event. (The sound seed is not necessary)
     */
    public static boolean containSound(@Nullable CompoundTag tag) {
        return tag != null && tag.contains(SOUND_EVENT);
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
        CompoundTag tag = soundShard.getTag();
        if (tag != null) {
            long newSeed = random.nextLong();
            tag.putLong(SOUND_SEED, newSeed);
            return newSeed;
        }
        return null;
    }

    /**
     * Add the cooldown to the player after using the sound shard.
     * <p>
     * Efficiency level will affect the cooldown time.
     */
    private void addCooldown(Player player, ItemStack soundShard) {
        int cd = DEFAULT_COOL_DOWN - 10 * Mth.clamp(soundShard.getEnchantmentLevel(Enchantments.BLOCK_EFFICIENCY), 0, 5);
        player.getCooldowns().addCooldown(this, cd);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack soundShard = player.getItemInHand(hand);
        addCooldown(player, soundShard);
        Info info = Info.ofItemStack(soundShard);
        if (info.sound() == null) {
            // start listening to the sound event
            if (level.isClientSide) {
                Listener.start();
            }
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(soundShard);
        } else if (player.getAbilities().instabuild && player.isSecondaryUseActive()) {
            // creative mode only: shift+use to change the sound seed.
            Long newSeed = tryToChangeSoundSeed(soundShard, level.random);
            if (newSeed != null) {
                level.playSeededSound(null, player.getX(), player.getY(), player.getZ(), info.sound(), player.getSoundSource(), 1.0F, 1.0F, newSeed);
            }
            return InteractionResultHolder.sidedSuccess(soundShard, level.isClientSide);
        } else {
            // play the sound event that saved in the sound shard
            level.playSeededSound(null, player.getX(), player.getY(), player.getZ(), info.sound(), player.getSoundSource(), 1.0F, 1.0F, info.seed() == null ? level.random.nextLong() : info.seed());
            return InteractionResultHolder.sidedSuccess(soundShard, level.isClientSide);
        }
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
            ResourceLocation location = Listener.getFinalResult();
            if (location != null) {
                player.displayClientMessage(getSoundEventTitle(location).withStyle(ChatFormatting.DARK_GREEN), true);
                // Send the sound event to the server to save it in the sound shard.
                CCNetwork.CHANNEL.sendToServer(new SoundShardPacket(player.getInventory().selected, location.toString()));
            }
        }
        super.releaseUsing(itemStack, level, livingEntity, tick);
    }

    @Override
    public void appendHoverText(ItemStack itemStack, @Nullable Level level, List<Component> components, TooltipFlag flag) {
        CompoundTag tag = itemStack.getTag();
        if (containSound(tag)) {
            ResourceLocation location = ResourceLocation.tryParse(tag.getString(SOUND_EVENT));
            if (location != null) {
                // Display the sound event that saved in the sound shard.
                // Green for the fixed seed, yellow for the random seed.
                ChatFormatting color = tag.contains(SOUND_SEED) ? ChatFormatting.DARK_GREEN : ChatFormatting.GOLD;
                components.add(getSoundEventTitle(location).withStyle(color));
            }
        }
        super.appendHoverText(itemStack, level, components, flag);
    }

    @Override
    public boolean isFoil(ItemStack itemStack) {
        return containSound(itemStack);
    }

    @Override
    public int getUseDuration(ItemStack itemStack) {
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
        public InteractionResult interact(BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand hand, ItemStack itemStack) {
            if (itemStack.is(CCMain.SOUND_SHARD_ITEM.get())) {
                CompoundTag tag = itemStack.getTag();
                if (containSound(tag)) {
                    tag.remove(SOUND_EVENT);
                    tag.remove(SOUND_SEED);
                    LayeredCauldronBlock.lowerFillLevel(blockState, level, blockPos);
                    level.playSound(null, blockPos, SoundEvents.POWDER_SNOW_FALL, SoundSource.BLOCKS, 1.0F, 1.0F);
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            }
            return InteractionResult.PASS;
        }
    }

    public record Info(@Nullable Holder<SoundEvent> sound, @Nullable Long seed) {
        public static Info ofItemStack(ItemStack soundShard) {
            CompoundTag tag = soundShard.getTag();
            if (tag == null) {
                return new Info(null, null);
            }
            Holder<SoundEvent> sound = tag.contains(SOUND_EVENT) ? getSoundEvent(tag.getString(SOUND_EVENT)) : null;
            Long seed = tag.contains(SOUND_SEED) ? tag.getLong(SOUND_SEED) : null;
            return new Info(sound, seed);
        }

        @Nullable
        private static Holder<SoundEvent> getSoundEvent(String event) {
            ResourceLocation location = ResourceLocation.tryParse(event);
            return location == null ? null : Holder.direct(SoundEvent.createVariableRangeEvent(location));
        }
    }
}