package io.github.c20c01.cc_mb.item;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.block.entity.MusicBoxBlockEntity;
import io.github.c20c01.cc_mb.util.player.TickPerBeat;
import net.minecraft.ChatFormatting;
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
    public Awl(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack awl = player.getItemInHand(hand);
        byte current = awl.getOrDefault(CCMain.TICK_PER_BEAT.get(), TickPerBeat.DEFAULT);
        byte next = (byte) (current + (player.isSecondaryUseActive() ? -1 : 1));
        if (next < TickPerBeat.MIN) {
            next = TickPerBeat.MAX;
        } else if (next > TickPerBeat.MAX) {
            next = TickPerBeat.MIN;
        }
        awl.set(CCMain.TICK_PER_BEAT.get(), next);
        player.displayClientMessage(Component.translatable(CCMain.TEXT_SET_TICK_PER_BEAT).append(String.valueOf(next)).withStyle(ChatFormatting.GOLD), true);
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
                player.displayClientMessage(Component.translatable(CCMain.TEXT_SET_TICK_PER_BEAT).append(tickPerBeat).withStyle(ChatFormatting.DARK_GREEN), true);
                return InteractionResult.CONSUME;
            }
            return InteractionResult.SUCCESS;
        }
        return super.useOn(context);
    }
}
