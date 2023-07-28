package io.github.c20c01.cc_mb.item;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.network.UpdateSoundShard;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundSourceEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault

public class SoundShard extends Item {
    public SoundShard() {
        super(new Item.Properties().stacksTo(1));

        CauldronInteraction.WATER.put(this, (blockState, level, blockPos, player, hand, itemStack) -> {
            if (itemStack.is(this)) {
                CompoundTag tag = itemStack.getTag();
                if (tag != null && tag.contains("SoundEvent")) {
                    if (!level.isClientSide) {
                        tag.remove("SoundEvent");
                        tag.remove("SoundSeed");
                        LayeredCauldronBlock.lowerFillLevel(blockState, level, blockPos);
                        level.playSound(null, blockPos, SoundEvents.PLAYER_SPLASH, SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            }
            return InteractionResult.PASS;
        });
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack soundShard = player.getItemInHand(hand);
        CompoundTag tag = soundShard.getOrCreateTag();

        player.getCooldowns().addCooldown(this, 20);

        if (tag.contains("SoundEvent")) {
            long soundSeed;
            // 按下shift从声音事件的多种声音里再重新抽取一种
            if (player.isShiftKeyDown()) {
                soundSeed = level.random.nextLong();
                tag.putLong("SoundSeed", soundSeed);
            } else {
                soundSeed = tag.getLong("SoundSeed");
            }

            // 播放储存的声音
            ResourceLocation location = ResourceLocation.tryParse(tag.getString("SoundEvent"));
            if (location != null) {
                level.playSeededSound((null), player.getX(), player.getY(), player.getZ(), SoundEvent.createVariableRangeEvent(location), player.getSoundSource(), 1.0F, 1.0F, soundSeed);
                if (level.isClientSide) {
                    MutableComponent title = getSoundEventTitle(location).withStyle(ChatFormatting.DARK_GREEN);
                    player.displayClientMessage(title, true);
                }
                return InteractionResultHolder.sidedSuccess(soundShard, level.isClientSide);
            }
        } else {
            // 开始收音
            if (level.isClientSide) Listener.start();
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(soundShard);
        }

        return super.use(level, player, hand);
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack itemStack, int tick) {
        if (livingEntity instanceof Player player && level.isClientSide) {
            ResourceLocation location = Listener.getLocation();
            if (location != null) {
                player.displayClientMessage(getSoundEventTitle(location).withStyle(ChatFormatting.GOLD), Boolean.TRUE);
            }
        }
        super.onUseTick(level, livingEntity, itemStack, tick);
    }

    @Override
    public void releaseUsing(ItemStack itemStack, Level level, LivingEntity livingEntity, int tick) {
        if (livingEntity instanceof Player player && level.isClientSide) {
            ResourceLocation location = Listener.getFinalResult();
            if (location != null) {
                UpdateSoundShard.toServer(player, location.toString());
            }
        }
        super.releaseUsing(itemStack, level, livingEntity, tick);
    }

    @Override
    public void appendHoverText(ItemStack itemStack, @Nullable Level level, List<Component> components, TooltipFlag flag) {
        CompoundTag tag = itemStack.getOrCreateTag();
        if (tag.contains("SoundEvent")) {
            ResourceLocation location = ResourceLocation.tryParse(tag.getString("SoundEvent"));
            if (location != null) {
                components.add(getSoundEventTitle(location).withStyle(ChatFormatting.DARK_GREEN));
            }
        }
        super.appendHoverText(itemStack, level, components, flag);
    }

    @Override
    public boolean isFoil(ItemStack itemStack) {
        return hasSound(itemStack) || super.isFoil(itemStack);
    }

    public static boolean hasSound(ItemStack itemStack) {
        return itemStack.getTag() != null && itemStack.getTag().contains("SoundEvent");
    }

    @Override
    public int getUseDuration(ItemStack itemStack) {
        return 600;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack itemStack) {
        return UseAnim.BOW;
    }

    private static MutableComponent getSoundEventTitle(ResourceLocation location) {
        var sound = Minecraft.getInstance().getSoundManager().getSoundEvent(location);
        MutableComponent result = Component.literal("???");

        if (sound != null) {
            Component subtitle = sound.getSubtitle();
            if (subtitle != null) {
                result = MutableComponent.create(subtitle.getContents());
            }
        }
        return result;
    }

    @Mod.EventBusSubscriber(modid = CCMain.ID, value = Dist.CLIENT)
    private static class Listener {
        private static Listener listener;
        private boolean changed = false;
        private ResourceLocation soundLocation = null;

        public Listener() {
            listener = this;
        }

        @SubscribeEvent
        public void listen(PlaySoundSourceEvent event) {
            changed = true;
            soundLocation = event.getSound().getLocation();
        }

        protected static void start() {
            MinecraftForge.EVENT_BUS.register(new Listener());
        }

        @Nullable
        protected static ResourceLocation getLocation() {
            if (listener == null || !listener.changed) {
                return null;
            } else {
                ResourceLocation location = listener.soundLocation;
                listener.changed = false;
                return location;
            }
        }

        @Nullable
        protected static ResourceLocation getFinalResult() {
            if (listener == null) {
                return null;
            } else {
                ResourceLocation location = listener.soundLocation;
                MinecraftForge.EVENT_BUS.unregister(listener);
                listener = null;
                return location;
            }
        }
    }
}