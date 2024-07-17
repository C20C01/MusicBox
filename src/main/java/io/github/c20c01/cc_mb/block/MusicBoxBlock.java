package io.github.c20c01.cc_mb.block;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.block.entity.MusicBoxBlockEntity$;
import io.github.c20c01.cc_mb.data.NoteGridData$;
import io.github.c20c01.cc_mb.item.Awl;
import io.github.c20c01.cc_mb.item.NoteGrid;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.item.Items;
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

import javax.annotation.Nullable;

public class MusicBoxBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty HAS_NOTE_GRID = BooleanProperty.create("has_note_grid");
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final EnumProperty<NoteBlockInstrument> INSTRUMENT = BlockStateProperties.NOTEBLOCK_INSTRUMENT;

    public MusicBoxBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).sound(SoundType.WOOD).strength(0.8F).ignitedByLava());
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HAS_NOTE_GRID, false)
                .setValue(POWERED, false)
                .setValue(INSTRUMENT, NoteBlockInstrument.HARP)
        );
    }

    /**
     * 类似于音符盒，根据下方方块的乐器类型设置当前乐器类型，头颅（需要放在音符盒上面才能生效的）无效。
     */
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
        return this.setInstrument(level, blockPos, this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(POWERED, level.hasNeighborSignal(blockPos))
        );
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState blockState, Level level, BlockPos blockPos, Block block, BlockPos blockPos1, boolean b) {
        BlockUtil.changeProperty(level, blockPos, blockState, POWERED, level.hasNeighborSignal(blockPos), Block.UPDATE_CLIENTS);
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasAnalogOutputSignal(BlockState blockState) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos blockPos) {
        if (blockState.getValue(HAS_NOTE_GRID)) {
            BlockEntity blockentity = level.getBlockEntity(blockPos);
            return blockentity instanceof MusicBoxBlockEntity$ be ? be.getSignal() : 0;
        } else {
            return 0;
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HAS_NOTE_GRID, POWERED, INSTRUMENT);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new MusicBoxBlockEntity$(blockPos, blockState);
    }

    @Override
    @SuppressWarnings("deprecation")
    public RenderShape getRenderShape(BlockState blockState) {
        return RenderShape.MODEL;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void attack(BlockState blockState, Level level, BlockPos blockPos, Player player) {
        // TODO
        if (!blockState.getValue(POWERED) && blockState.getValue(HAS_NOTE_GRID)) {
            MusicBoxBlockEntity$ blockEntity = (MusicBoxBlockEntity$) level.getBlockEntity(blockPos);
            if (blockEntity != null) {
                blockEntity.playOneBeat(level, blockPos, blockState);
            }
        }
        super.attack(blockState, level, blockPos, player);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        ItemStack itemStack = player.getItemInHand(hand);
        MusicBoxBlockEntity$ musicBoxBlockEntity = level.getBlockEntity(blockPos) instanceof MusicBoxBlockEntity$ be ? be : null;
        if (musicBoxBlockEntity == null) {
            return super.use(blockState, level, blockPos, player, hand, hitResult);
        }

        if (itemStack.is(CCMain.AWL_ITEM.get())) {
            // 调节每拍所用的tick数
            byte tickPerBeat = Awl.getTickPerBeatTag(itemStack.getOrCreateTag());
            musicBoxBlockEntity.setTickPerBeat(level, blockPos, tickPerBeat);
            player.displayClientMessage(Component.translatable(CCMain.TEXT_CHANGE_TICK_PER_BEAT).append(String.valueOf(musicBoxBlockEntity.getTickPerBeat())).withStyle(ChatFormatting.DARK_AQUA), Boolean.TRUE);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (blockState.getValue(HAS_NOTE_GRID)) {
            if (player.isSecondaryUseActive()) {
                // 取出纸带
                ItemStack out = musicBoxBlockEntity.removeItem();
                if (!out.isEmpty()) {
                    ItemHandlerHelper.giveItemToPlayer(player, out);
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }

            } else if (!blockState.getValue(POWERED)) {
                // 演奏一拍
                musicBoxBlockEntity.playOneBeat(level, blockPos, blockState);
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        } else {
            // 放入纸带
            if (itemStack.is(CCMain.NOTE_GRID_ITEM.get())) {
                // TODO REMOVE
                NoteGrid.setId(itemStack, 0);
                // TODO REMOVE

                if (level.isClientSide) {
                    return InteractionResult.SUCCESS;
                } else if (musicBoxBlockEntity.setItem(itemStack)) {
                    itemStack.shrink(1);
                    return InteractionResult.CONSUME;
                }
            }

            // TODO REMOVE
            if (itemStack.is(Items.WRITABLE_BOOK)) {
                var data = NoteGridData$.ofBook(itemStack);
                if (level instanceof ServerLevel serverLevel) {
                    data.save(serverLevel.getServer(), 0);
                    player.displayClientMessage(Component.literal("书本所存纸带数据已保存至世界数据中").withStyle(ChatFormatting.DARK_AQUA), true);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            // TODO REMOVE

        }

        return super.use(blockState, level, blockPos, player, hand, hitResult);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos blockPos, BlockState blockState, @Nullable LivingEntity livingEntity, ItemStack itemStack) {
        // TODO
        super.setPlacedBy(level, blockPos, blockState, livingEntity, itemStack);
        CompoundTag compoundTag = BlockItem.getBlockEntityData(itemStack);
        if (compoundTag != null && compoundTag.contains(MusicBoxBlockEntity$.NOTE_GRID) && !ItemStack.of(compoundTag.getCompound(MusicBoxBlockEntity$.NOTE_GRID)).isEmpty()) {
            BlockUtil.changeProperty(level, blockPos, blockState, HAS_NOTE_GRID, true);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState blockState, Level level, BlockPos blockPos, BlockState blockState1, boolean b) {
        // TODO
        if (!blockState.is(blockState1.getBlock())) {
            if (level.getBlockEntity(blockPos) instanceof MusicBoxBlockEntity$ blockEntity) {
                Containers.dropItemStack(level, blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockEntity.getItem());
            }
        }
        super.onRemove(blockState, level, blockPos, blockState1, b);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pType) {
        if (pState.getValue(POWERED) && pState.getValue(HAS_NOTE_GRID)) {
            return createTickerHelper(pType, CCMain.MUSIC_BOX_BLOCK_ENTITY.get(), MusicBoxBlockEntity$::tick);
        }
        return null;
    }
}
