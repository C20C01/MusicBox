package io.github.c20c01.cc_mb.item;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.block.MusicBoxBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault

public class Awl extends Item {
    public Awl() {
        super(new Properties().durability(256));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack awl = player.getItemInHand(hand);
        CompoundTag tag = awl.getOrCreateTag();

        byte tickPerBeat = getTickPerBeatTag(tag);
        tickPerBeat += player.isShiftKeyDown() ? -1 : 1;

        if (tickPerBeat < MusicBoxBlockEntity.MIN_TICK_PER_BEAT) {
            tickPerBeat = MusicBoxBlockEntity.MAX_TICK_PER_BEAT;
        } else if (tickPerBeat > MusicBoxBlockEntity.MAX_TICK_PER_BEAT) {
            tickPerBeat = MusicBoxBlockEntity.MIN_TICK_PER_BEAT;
        }

        setTickPerBeatTag(tag, tickPerBeat);
        player.displayClientMessage(Component.translatable(CCMain.TEXT_SET_TICK_PER_BEAT).append(String.valueOf(tickPerBeat)).withStyle(ChatFormatting.GOLD), Boolean.TRUE);
        return InteractionResultHolder.sidedSuccess(awl, level.isClientSide());
    }

    private static void setTickPerBeatTag(CompoundTag tag, byte tickPerBeat) {
        tag.putByte("TickPerBeat", tickPerBeat);
    }

    public static byte getTickPerBeatTag(CompoundTag tag) {
        return tag.contains("TickPerBeat") ? tag.getByte("TickPerBeat") : 10;
    }
}
