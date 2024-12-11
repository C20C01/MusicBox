package io.github.c20c01.cc_mb.item;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.block.entity.MusicBoxBlockEntity;
import io.github.c20c01.cc_mb.util.player.TickPerBeat;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class Awl extends Item {
    private static final String TICK_PER_BEAT_KEY = "tick_per_beat";

    public Awl(Properties properties) {
        super(properties);
    }

    public static byte getTickPerBeatTag(CompoundTag tag) {
        return tag.contains(TICK_PER_BEAT_KEY) ? tag.getByte(TICK_PER_BEAT_KEY) : TickPerBeat.DEFAULT;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack awl = player.getItemInHand(hand);
        CompoundTag tag = awl.getOrCreateTag();

        byte nextTickPerBeat = (byte) (getTickPerBeatTag(tag) + (player.isSecondaryUseActive() ? -1 : 1));
        if (nextTickPerBeat < TickPerBeat.MIN) {
            nextTickPerBeat = TickPerBeat.MAX;
        } else if (nextTickPerBeat > TickPerBeat.MAX) {
            nextTickPerBeat = TickPerBeat.MIN;
        }
        tag.putByte(TICK_PER_BEAT_KEY, nextTickPerBeat);
        player.displayClientMessage(Component.translatable(CCMain.TEXT_TICK_PER_BEAT).append(String.valueOf(nextTickPerBeat)).withStyle(ChatFormatting.GOLD), true);
        return InteractionResultHolder.sidedSuccess(awl, level.isClientSide());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // check tick per beat
        Level level = context.getLevel();
        if (level.getBlockEntity(context.getClickedPos()) instanceof MusicBoxBlockEntity blockEntity) {
            Player player = context.getPlayer();
            if (player != null && !level.isClientSide) {
                String tickPerBeat = String.valueOf(blockEntity.getTickPerBeat());
                player.displayClientMessage(Component.translatable(CCMain.TEXT_TICK_PER_BEAT).append(tickPerBeat).withStyle(ChatFormatting.DARK_GREEN), true);
                return InteractionResult.CONSUME;
            }
            return InteractionResult.SUCCESS;
        }
        return super.useOn(context);
    }
}
