package io.github.c20c01.cc_mb.item;

import io.github.c20c01.cc_mb.CCMain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

public class PaperPaste extends Item {
    public PaperPaste(Properties properties) {
        super(properties);
        CauldronInteraction.WATER.put(Items.PAPER, new MakePaperPaste());
    }

    /**
     * Make paper paste with paper and water.
     */
    private static class MakePaperPaste implements CauldronInteraction {
        @Override
        public InteractionResult interact(BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, ItemStack itemStack) {
            if (!itemStack.is(Items.PAPER)) {
                return InteractionResult.PASS;
            } else if (!level.isClientSide) {
                itemStack.shrink(1);
                level.playSound(null, blockPos, SoundEvents.SLIME_BLOCK_FALL, SoundSource.BLOCKS, 1.0F, 1.0F);
                player.addItem(new ItemStack(CCMain.PAPER_PASTE_ITEM.get(), 16));
                LayeredCauldronBlock.lowerFillLevel(blockState, level, blockPos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }
}
