package io.github.c20c01.cc_mb.block;

import com.mojang.serialization.MapCodec;
import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.block.entity.ControllerBoxBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class ControllerBoxBlock extends BaseBoxPlayerBlock {
    public static final BooleanProperty UNDER_SOUND_BOX = BooleanProperty.create("under_sound_box");
    public static final MapCodec<ControllerBoxBlock> CODEC = simpleCodec(ControllerBoxBlock::new);

    public ControllerBoxBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HAS_NOTE_GRID, false)
                .setValue(POWERED, false)
                .setValue(UNDER_SOUND_BOX, false)
        );
    }

    @Override
    public MapCodec<ControllerBoxBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockState updateShape(BlockState blockState, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos blockPos, BlockPos neighborPos) {
        if (direction != Direction.UP) {
            return super.updateShape(blockState, direction, neighborState, level, blockPos, neighborPos);
        }
        return blockState.setValue(UNDER_SOUND_BOX, neighborState.is(CCMain.SOUND_BOX_BLOCK.get()));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos blockPos = context.getClickedPos();
        boolean underSoundBox = level.getBlockState(blockPos.above()).is(CCMain.SOUND_BOX_BLOCK.get());
        return super.getStateForPlacement(context).setValue(UNDER_SOUND_BOX, underSoundBox);
    }

    @Override
    public void neighborChanged(BlockState blockState, Level level, BlockPos blockPos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        boolean powered = level.hasNeighborSignal(blockPos);
        if (powered != blockState.getValue(POWERED)) {
            if (powered && blockState.getValue(HAS_NOTE_GRID) && level.getBlockEntity(blockPos) instanceof ControllerBoxBlockEntity controllerBlockEntity) {
                controllerBlockEntity.nextBeat(level, blockPos, blockState);
            }
            level.setBlock(blockPos, blockState.setValue(POWERED, powered), UPDATE_ALL_IMMEDIATE);
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack itemStack, BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        ControllerBoxBlockEntity blockEntity = level.getBlockEntity(blockPos) instanceof ControllerBoxBlockEntity be ? be : null;
        if (blockEntity == null) {
            return super.useItemOn(itemStack, blockState, level, blockPos, player, hand, hitResult);
        }

        if (blockState.getValue(HAS_NOTE_GRID)) {
            if (player.isSecondaryUseActive()) {
                return takeOutNoteGrid(level, blockEntity, player);
            }
            blockEntity.nextBeat(level, blockPos, blockState);
            return ItemInteractionResult.SUCCESS;
        } else {
            if (blockEntity.canPlaceItem(itemStack)) {
                return putInNoteGrid(level, blockPos, blockEntity, itemStack);
            }
        }

        return super.useItemOn(itemStack, blockState, level, blockPos, player, hand, hitResult);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(UNDER_SOUND_BOX);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new ControllerBoxBlockEntity(blockPos, blockState);
    }
}
