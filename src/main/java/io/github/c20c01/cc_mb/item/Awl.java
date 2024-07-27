package io.github.c20c01.cc_mb.item;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.block.entity.MusicBoxBlockEntity;
import io.github.c20c01.cc_mb.util.player.AbstractNoteGridPlayer;
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
    private static final String TICK_PER_BEAT_KEY = "TickPerBeat";

    public Awl(Properties properties) {
        super(properties);
    }

    public static byte getTickPerBeatTag(CompoundTag tag) {
        return tag.contains(TICK_PER_BEAT_KEY) ? tag.getByte(TICK_PER_BEAT_KEY) : AbstractNoteGridPlayer.getDefaultTickPerBeat();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack awl = player.getItemInHand(hand);
        CompoundTag tag = awl.getOrCreateTag();

        byte nextTickPerBeat = (byte) (getTickPerBeatTag(tag) + (player.isSecondaryUseActive() ? -1 : 1));
        if (nextTickPerBeat < AbstractNoteGridPlayer.MIN_TICK_PER_BEAT) {
            nextTickPerBeat = AbstractNoteGridPlayer.MAX_TICK_PER_BEAT;
        } else if (nextTickPerBeat > AbstractNoteGridPlayer.MAX_TICK_PER_BEAT) {
            nextTickPerBeat = AbstractNoteGridPlayer.MIN_TICK_PER_BEAT;
        }
        tag.putByte(TICK_PER_BEAT_KEY, nextTickPerBeat);
        player.displayClientMessage(Component.translatable(CCMain.TEXT_SET_TICK_PER_BEAT).append(String.valueOf(nextTickPerBeat)).withStyle(ChatFormatting.GOLD), true);
        return InteractionResultHolder.sidedSuccess(awl, level.isClientSide());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // check tick per beat
        Level level = context.getLevel();
        if (level.getBlockEntity(context.getClickedPos()) instanceof MusicBoxBlockEntity blockEntity) {
            Player player = context.getPlayer();
            if (player != null) {
                String tickPerBeat = String.valueOf(blockEntity.getTickPerBeat());
                player.displayClientMessage(Component.translatable(CCMain.TEXT_SET_TICK_PER_BEAT).append(tickPerBeat).withStyle(ChatFormatting.DARK_GREEN), true);
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
        }
        return super.useOn(context);
    }
}
