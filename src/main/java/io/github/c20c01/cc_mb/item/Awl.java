package io.github.c20c01.cc_mb.item;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.block.entity.MusicBoxBlockEntity;
import io.github.c20c01.cc_mb.util.player.TickPerBeat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

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
        byte current = getTickPerBeatTag(tag);
        int next;
        if (player.isSecondaryUseActive()) {
            // decrease octave
            next = current > TickPerBeat.MIN ? current - 1 : TickPerBeat.MAX;
        } else {
            // increase octave
            next = current < TickPerBeat.MAX ? current + 1 : TickPerBeat.MIN;
        }
        tag.putByte(TICK_PER_BEAT_KEY, (byte) next);
        player.displayClientMessage(Component.translatable(CCMain.TEXT_TICK_PER_BEAT).append(String.valueOf(next)).withStyle(ChatFormatting.GOLD), true);
        return InteractionResultHolder.sidedSuccess(awl, level.isClientSide);
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

    /**
     * Stops the creative player from breaking the music box with the awl.
     */
    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos blockPos, Player player) {
        if (level.getBlockEntity(blockPos) instanceof MusicBoxBlockEntity blockEntity && player.getAbilities().instabuild) {
            if (level instanceof ServerLevel serverLevel) {
                blockEntity.setOctave(serverLevel, blockPos, player);
            }
            return false;
        }
        return super.canAttackBlock(state, level, blockPos, player);
    }
}