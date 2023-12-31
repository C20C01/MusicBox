package io.github.c20c01.cc_mb.block;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.item.Awl;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
        BlockState blockBelow = levelAccessor.getBlockState(blockPos.below());
        NoteBlockInstrument instrument = blockBelow.instrument();
        boolean flag = instrument.worksAboveNoteBlock() && !blockBelow.is(CCMain.SOUND_BOX_BLOCK.get());
        return blockState.setValue(INSTRUMENT, flag ? NoteBlockInstrument.HARP : instrument);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState blockState, Direction direction, BlockState blockState1, LevelAccessor levelAccessor, BlockPos blockPos, BlockPos blockPos1) {
        boolean flag = direction == Direction.DOWN;
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
        CCUtil.changeProperty(level, blockPos, blockState, POWERED, level.hasNeighborSignal(blockPos));
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
                blockEntity.tryToPlayOneBeat(serverLevel, blockPos, blockState);
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

        if (itemStack.is(CCMain.AWL_ITEM.get())) {
            // 调节每拍所用的tick数
            byte tickPerBeat = Awl.getTickPerBeatTag(itemStack.getOrCreateTag());
            blockEntity.setTickPerBeat(tickPerBeat);
            player.displayClientMessage(Component.translatable(CCMain.TEXT_CHANGE_TICK_PER_BEAT).append(String.valueOf(blockEntity.getTickPerBeat())).withStyle(ChatFormatting.DARK_AQUA), Boolean.TRUE);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (blockState.getValue(EMPTY)) {
            // 放入纸带
            if (itemStack.is(CCMain.NOTE_GRID_ITEM.get())) {
                if (level.isClientSide) {
                    return InteractionResult.SUCCESS;
                } else if (blockEntity.setNoteGrid(itemStack)) {
                    itemStack.shrink(1);
                    return InteractionResult.CONSUME;
                }
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
                    blockEntity.tryToPlayOneBeat(serverLevel, blockPos, blockState);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }

        return super.use(blockState, level, blockPos, player, hand, hitResult);
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
                Containers.dropItemStack(level, blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockEntity.getFirstItem());
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
