package io.github.c20c01.cc_mb.block;

import io.github.c20c01.cc_mb.block.entity.PuncherBoxBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.ticks.TickPriority;

import javax.annotation.Nullable;

public class PuncherBoxBlock extends Block implements EntityBlock, NoteGridBoxBlock {
    public PuncherBoxBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(HAS_NOTE_GRID, false)
                .setValue(POWERED, false)
        );
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new PuncherBoxBlockEntity(blockPos, blockState);
    }

    @Override
    protected void tick(BlockState blockState, ServerLevel level, BlockPos blockPos, RandomSource random) {
        if (!(level.getBlockEntity(blockPos) instanceof PuncherBoxBlockEntity puncherBox)) return;

        int power = level.getBestNeighborSignal(blockPos);
        boolean shouldBePowered = power > 0;

        if (blockState.getValue(POWERED) == shouldBePowered) {
            // get s rise signal that only holds for one tick
            if (!shouldBePowered && !puncherBox.isEmpty()) {
                puncherBox.onFinished();
            }
        } else {
            if (shouldBePowered && !puncherBox.isEmpty()) {
                puncherBox.trigger(power);
            }
            level.setBlock(blockPos, blockState.setValue(POWERED, shouldBePowered), Block.UPDATE_ALL);
        }
    }

    private void tryUpdatePower(Level level, BlockPos blockPos, BlockState blockState) {
        if (level.isClientSide()) return;
        if (level.getBlockTicks().willTickThisTick(blockPos, this)) return;
        if (blockState.getValue(POWERED) != level.hasNeighborSignal(blockPos)) {
            // EXTREMELY_HIGH to make sure it can be updated by high frequency signal source
            level.scheduleTick(blockPos, this, 2, TickPriority.EXTREMELY_HIGH);
        }
    }

    @Override
    protected void neighborChanged(BlockState blockState, Level level, BlockPos blockPos, Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston) {
        tryUpdatePower(level, blockPos, blockState);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState blockState) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos blockPos, Direction direction) {
        return getOutputSignal(level, blockPos, blockState);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HAS_NOTE_GRID, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    public RenderShape getRenderShape(BlockState blockState) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack itemStack, BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(blockPos) instanceof PuncherBoxBlockEntity puncherBox)) {
            return super.useItemOn(itemStack, blockState, level, blockPos, player, hand, hitResult);
        }

        if (blockState.getValue(HAS_NOTE_GRID)) {
            if (player.isSecondaryUseActive()) return takeOutNoteGrid(level, puncherBox, player.getInventory());
            return toNextBeat(level, blockPos, puncherBox);
        } else {
            return putInNoteGrid(level, blockPos, puncherBox, itemStack);
        }
    }

    private InteractionResult toNextBeat(Level level, BlockPos blockPos, PuncherBoxBlockEntity puncherBox) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        level.playSound(null, blockPos, SoundEvents.SPYGLASS_USE, SoundSource.PLAYERS);
        puncherBox.trigger(1);// jump to the next beat without punching
        return InteractionResult.CONSUME;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos blockPos, BlockState blockState, @Nullable LivingEntity livingEntity, ItemStack itemStack) {
        super.setPlacedBy(level, blockPos, blockState, livingEntity, itemStack);
        if (level.getBlockEntity(blockPos) instanceof PuncherBoxBlockEntity puncherBox && !puncherBox.isEmpty()) {
            level.setBlock(blockPos, blockState.setValue(HAS_NOTE_GRID, true), Block.UPDATE_CLIENTS);
        }
        tryUpdatePower(level, blockPos, blockState);
    }
}
