package io.github.c20c01.cc_mb.block;

import io.github.c20c01.cc_mb.CCMain;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault

public class MusicBoxBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty EMPTY = BooleanProperty.create("empty");
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final EnumProperty<NoteBlockInstrument> INSTRUMENT = BlockStateProperties.NOTEBLOCK_INSTRUMENT;

    public MusicBoxBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).sound(SoundType.WOOD).strength(0.8F).ignitedByLava());
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(EMPTY, Boolean.TRUE).setValue(POWERED, Boolean.FALSE).setValue(INSTRUMENT, NoteBlockInstrument.HARP));
    }

    private BlockState setInstrument(LevelAccessor levelAccessor, BlockPos blockPos, BlockState blockState) {
        NoteBlockInstrument noteblockinstrument = levelAccessor.getBlockState(blockPos.above()).instrument();
        if (noteblockinstrument.worksAboveNoteBlock()) {
            return blockState.setValue(INSTRUMENT, noteblockinstrument);
        } else {
            NoteBlockInstrument instrument1 = levelAccessor.getBlockState(blockPos.below()).instrument();
            NoteBlockInstrument instrument2 = instrument1.worksAboveNoteBlock() ? NoteBlockInstrument.HARP : instrument1;
            return blockState.setValue(INSTRUMENT, instrument2);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState blockState, Direction direction, BlockState blockState1, LevelAccessor levelAccessor, BlockPos blockPos, BlockPos blockPos1) {
        boolean flag = direction.getAxis() == Direction.Axis.Y;
        return flag ? this.setInstrument(levelAccessor, blockPos, blockState) : super.updateShape(blockState, direction, blockState1, levelAccessor, blockPos, blockPos1);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos blockPos = context.getClickedPos();
        return this.setInstrument(level, blockPos, this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(POWERED, level.hasNeighborSignal(blockPos)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState blockState, Level level, BlockPos blockPos, Block block, BlockPos blockPos1, boolean b) {
        changeProperty(level, blockPos, blockState, POWERED, level.hasNeighborSignal(blockPos));
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasAnalogOutputSignal(BlockState blockState) {
        return true;
    }

    /**
     * @return 没有纸带为 0，有纸带但不发音为 1，发音时为 min(音符音高 + 2, 15)
     */
    @Override
    @SuppressWarnings("deprecation")
    public int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos blockPos) {
        if (blockState.getValue(EMPTY)) {
            return 0;
        } else {
            BlockEntity blockentity = level.getBlockEntity(blockPos);
            return blockentity instanceof MusicBoxBlockEntity ? ((MusicBoxBlockEntity) blockentity).getAnalogOutputSignal() : 0;
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, EMPTY, POWERED, INSTRUMENT);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new MusicBoxBlockEntity(blockPos, blockState);
    }

    @Override
    public RenderShape getRenderShape(BlockState blockState) {
        return RenderShape.MODEL;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void attack(BlockState blockState, Level level, BlockPos blockPos, Player player) {
        if (level instanceof ServerLevel serverLevel && !blockState.getValue(POWERED) && !blockState.getValue(EMPTY)) {
            MusicBoxBlockEntity blockEntity = (MusicBoxBlockEntity) level.getBlockEntity(blockPos);
            if (blockEntity != null) {
                blockEntity.playOneBeat(serverLevel, blockPos, blockState);
            }
        }
        super.attack(blockState, level, blockPos, player);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        ItemStack itemStack = player.getItemInHand(hand);
        MusicBoxBlockEntity blockEntity = (MusicBoxBlockEntity) level.getBlockEntity(blockPos);
        if (blockEntity == null) {
            return super.use(blockState, level, blockPos, player, hand, hitResult);
        }

        if (blockState.getValue(EMPTY)) {
            // 放入纸带
            if (itemStack.is(CCMain.NOTE_GRID_ITEM.get()) && blockEntity.setNoteGrid(itemStack)) {
                if (!player.getAbilities().instabuild) {
                    itemStack.setCount(0);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }

        } else {
            if (player.isShiftKeyDown()) {
                // 取出纸带
                ItemStack out = blockEntity.outNoteGrid();
                if (!out.isEmpty()) {
                    ItemHandlerHelper.giveItemToPlayer(player, out);
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }

            } else if (!blockState.getValue(POWERED)) {
                // 演奏一拍
                if (level instanceof ServerLevel serverLevel) {
                    blockEntity.playOneBeat(serverLevel, blockPos, blockState);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }

        return super.use(blockState, level, blockPos, player, hand, hitResult);
    }

    public static <T extends Comparable<T>, V extends T> void changeProperty(Level level, BlockPos blockPos, BlockState blockState, Property<T> property, V value) {
        if (!blockState.getValue(property).equals(value)) {
            level.setBlockAndUpdate(blockPos, blockState.setValue(property, value));
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos blockPos, BlockState blockState, @Nullable LivingEntity livingEntity, ItemStack itemStack) {
        super.setPlacedBy(level, blockPos, blockState, livingEntity, itemStack);
        CompoundTag compoundTag = BlockItem.getBlockEntityData(itemStack);
        if (compoundTag != null && compoundTag.contains("NoteGrid") && !ItemStack.of(compoundTag.getCompound("NoteGrid")).isEmpty()) {
            level.setBlock(blockPos, blockState.setValue(EMPTY, Boolean.FALSE), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState blockState, Level level, BlockPos blockPos, BlockState blockState1, boolean b) {
        if (!blockState.is(blockState1.getBlock())) {
            if (level.getBlockEntity(blockPos) instanceof MusicBoxBlockEntity blockEntity) {
                Containers.dropItemStack(level, blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockEntity.getNoteGrid());
            }
        }
        super.onRemove(blockState, level, blockPos, blockState1, b);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> blockEntityType) {
        return level.isClientSide ? null : createTickerHelper(blockEntityType, CCMain.MUSIC_BOX_BLOCK_ENTITY.get(), MusicBoxBlockEntity::playTick);
    }
}
