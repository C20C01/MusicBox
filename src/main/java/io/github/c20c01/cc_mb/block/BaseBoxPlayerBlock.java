package io.github.c20c01.cc_mb.block;

import io.github.c20c01.cc_mb.block.entity.BaseBoxPlayerBlockEntity;
import io.github.c20c01.cc_mb.util.BlockUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;

public abstract class BaseBoxPlayerBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty HAS_NOTE_GRID = BooleanProperty.create("has_note_grid");
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    protected BaseBoxPlayerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos blockPos = context.getClickedPos();
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(POWERED, level.hasNeighborSignal(blockPos));
    }

    @Override
    public void neighborChanged(BlockState blockState, Level level, BlockPos blockPos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        BlockUtils.changeProperty(level, blockPos, blockState, POWERED, level.hasNeighborSignal(blockPos), UPDATE_ALL_IMMEDIATE);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState blockState) {
        return true;
    }

    protected final ItemInteractionResult takeOutNoteGrid(Level level, BaseBoxPlayerBlockEntity<?> blockEntity, Player player) {
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        ItemHandlerHelper.giveItemToPlayer(player, blockEntity.removeItem());
        return ItemInteractionResult.CONSUME;
    }

    protected final ItemInteractionResult putInNoteGrid(Level level, BlockPos blockPos, BaseBoxPlayerBlockEntity<?> blockEntity, ItemStack itemStack) {
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        blockEntity.setItem(itemStack);
        itemStack.shrink(1);// creative mode also need to shrink
        level.playSound(null, blockPos, SoundEvents.BOOK_PUT, SoundSource.BLOCKS);
        return ItemInteractionResult.CONSUME;
    }

    @Override
    public int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos blockPos) {
        if (blockState.getValue(HAS_NOTE_GRID)) {
            BlockEntity blockentity = level.getBlockEntity(blockPos);
            return blockentity instanceof BaseBoxPlayerBlockEntity<?> be ? be.getSignal() : 0;
        } else {
            return 0;
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HAS_NOTE_GRID, POWERED);
    }

    @Override
    public RenderShape getRenderShape(BlockState blockState) {
        return RenderShape.MODEL;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos blockPos, BlockState blockState, @Nullable LivingEntity livingEntity, ItemStack itemStack) {
        super.setPlacedBy(level, blockPos, blockState, livingEntity, itemStack);
        CustomData customdata = itemStack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
        if (customdata.contains(BaseBoxPlayerBlockEntity.NOTE_GRID)) {
            BlockUtils.changeProperty(level, blockPos, blockState, HAS_NOTE_GRID, true, UPDATE_CLIENTS);
        }
    }

    @Override
    public void onRemove(BlockState blockState, Level level, BlockPos blockPos, BlockState blockState1, boolean b) {
        if (!blockState.is(blockState1.getBlock())) {
            if (level.getBlockEntity(blockPos) instanceof BaseBoxPlayerBlockEntity<?> blockEntity) {
                Containers.dropItemStack(level, blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockEntity.getItem());
            }
        }
        super.onRemove(blockState, level, blockPos, blockState1, b);
    }
}
