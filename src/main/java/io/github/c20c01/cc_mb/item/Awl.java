package io.github.c20c01.cc_mb.item;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.block.entity.MusicBoxBlockEntity;
import io.github.c20c01.cc_mb.util.player.TickPerBeat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class Awl extends Item {
    public Awl(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack awl = player.getItemInHand(hand);
        byte current = awl.getOrDefault(CCMain.TICK_PER_BEAT.get(), TickPerBeat.DEFAULT);
        int next;
        if (player.isSecondaryUseActive()) {
            // decrease octave
            next = current > TickPerBeat.MIN ? current - 1 : TickPerBeat.MAX;
        } else {
            // increase octave
            next = current < TickPerBeat.MAX ? current + 1 : TickPerBeat.MIN;
        }
        awl.set(CCMain.TICK_PER_BEAT.get(), (byte) next);
        player.displayClientMessage(Component.translatable(CCMain.TEXT_TICK_PER_BEAT).append(String.valueOf(next)).withStyle(ChatFormatting.GOLD), true);
        return InteractionResult.SUCCESS_SERVER;
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
    public boolean canDestroyBlock(ItemStack stack, BlockState state, Level level, BlockPos blockPos, LivingEntity entity) {
        if (level.getBlockEntity(blockPos) instanceof MusicBoxBlockEntity blockEntity && entity instanceof Player player && player.getAbilities().instabuild) {
            if (level instanceof ServerLevel serverLevel) {
                blockEntity.setOctave(serverLevel, blockPos, player);
            }
            return false;
        }
        return super.canDestroyBlock(stack, state, level, blockPos, entity);
    }
}