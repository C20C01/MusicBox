package io.github.c20c01.cc_mb.block;

import com.mojang.serialization.MapCodec;
import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.block.entity.MusicBoxBlockEntity;
import io.github.c20c01.cc_mb.item.Awl;
import io.github.c20c01.cc_mb.util.BlockUtils;
import io.github.c20c01.cc_mb.util.player.TickPerBeat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;

public class MusicBoxBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty HAS_NOTE_GRID = BooleanProperty.create("has_note_grid");
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final EnumProperty<NoteBlockInstrument> INSTRUMENT = BlockStateProperties.NOTEBLOCK_INSTRUMENT;
    public static final MapCodec<MusicBoxBlock> CODEC = simpleCodec(MusicBoxBlock::new);

    public MusicBoxBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HAS_NOTE_GRID, false)
                .setValue(POWERED, false)
                .setValue(INSTRUMENT, NoteBlockInstrument.HARP)
        );
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    /**
     * Only support instruments that {@link NoteBlockInstrument#isTunable()}
     */
    private BlockState setInstrument(LevelAccessor levelAccessor, BlockPos blockPos, BlockState blockState) {
        BlockState blockBelow = levelAccessor.getBlockState(blockPos.below());
        NoteBlockInstrument instrument = blockBelow.instrument();
        if (instrument.hasCustomSound() && !blockBelow.is(CCMain.SOUND_BOX_BLOCK.get())) {
            // only sound box can have custom sound
            instrument = NoteBlockInstrument.HARP;
        }
        return blockState.setValue(INSTRUMENT, instrument);
    }

    @Override
    public BlockState updateShape(BlockState blockState, Direction direction, BlockState blockState1, LevelAccessor levelAccessor, BlockPos blockPos, BlockPos blockPos1) {
        return direction == Direction.DOWN ? this.setInstrument(levelAccessor, blockPos, blockState) : super.updateShape(blockState, direction, blockState1, levelAccessor, blockPos, blockPos1);
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
    public void neighborChanged(BlockState blockState, Level level, BlockPos blockPos, Block block, BlockPos blockPos1, boolean b) {
        BlockUtils.changeProperty(level, blockPos, blockState, POWERED, level.hasNeighborSignal(blockPos), UPDATE_ALL_IMMEDIATE);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState blockState) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos blockPos) {
        if (blockState.getValue(HAS_NOTE_GRID)) {
            BlockEntity blockentity = level.getBlockEntity(blockPos);
            return blockentity instanceof MusicBoxBlockEntity be ? be.getSignal() : 0;
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
        return new MusicBoxBlockEntity(blockPos, blockState);
    }

    @Override
    public RenderShape getRenderShape(BlockState blockState) {
        return RenderShape.MODEL;
    }

    /**
     * Attack with an awl in non-creative mode will set the octave,
     * without an awl will play the next beat.
     * For creative mode setting the octave, see {@link Awl#canAttackBlock}
     */
    @Override
    public void attack(BlockState blockState, Level level, BlockPos blockPos, Player player) {
        if (level.isClientSide || !(level.getBlockEntity(blockPos) instanceof MusicBoxBlockEntity blockEntity)) {
            return;
        }
        if (player.getItemInHand(InteractionHand.MAIN_HAND).is(CCMain.AWL_ITEM)) {
            blockEntity.setOctave((ServerLevel) level, blockPos, player);
            return;
        }

        blockEntity.playNextBeat((ServerLevel) level, blockPos, blockState);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack itemStack, BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        MusicBoxBlockEntity blockEntity = level.getBlockEntity(blockPos) instanceof MusicBoxBlockEntity be ? be : null;
        if (blockEntity == null) {
            return super.useItemOn(itemStack, blockState, level, blockPos, player, hand, hitResult);
        }

        if (itemStack.is(CCMain.AWL_ITEM.get()) && !player.isSecondaryUseActive()) {
            // modify tick per beat
            if (level.isClientSide) {
                return ItemInteractionResult.SUCCESS;
            }
            byte tickPerBeat = itemStack.getOrDefault(CCMain.TICK_PER_BEAT, TickPerBeat.DEFAULT);
            blockEntity.setTickPerBeat((ServerLevel) level, blockPos, tickPerBeat);
            level.playSound(null, blockPos, SoundEvents.SPYGLASS_USE, SoundSource.BLOCKS);
            player.displayClientMessage(Component.translatable(CCMain.TEXT_CHANGE_TICK_PER_BEAT).append(String.valueOf(blockEntity.getTickPerBeat())).withStyle(ChatFormatting.DARK_AQUA), true);
            return ItemInteractionResult.CONSUME;
        }

        if (blockState.getValue(HAS_NOTE_GRID)) {
            if (player.isSecondaryUseActive()) {
                // take out note grid
                if (level.isClientSide) {
                    return ItemInteractionResult.SUCCESS;
                }
                ItemHandlerHelper.giveItemToPlayer(player, blockEntity.removeItem());
                return ItemInteractionResult.CONSUME;
            }
            if (!blockState.getValue(POWERED)) {
                if (player.getAbilities().instabuild && itemStack.is(Items.WRITABLE_BOOK) || itemStack.is(CCMain.NOTE_GRID_ITEM.get())) {
                    // creative only: join the new data to the note grid
                    if (level.isClientSide) {
                        return ItemInteractionResult.SUCCESS;
                    }
                    if (blockEntity.joinData(itemStack)) {
                        blockEntity.ejectNoteGrid(level, blockPos, blockState);
                        level.playSound(null, blockPos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS);
                    }
                    return ItemInteractionResult.CONSUME;
                }
                // play one beat
                if (level.isClientSide) {
                    return ItemInteractionResult.SUCCESS;
                }
                blockEntity.playNextBeat((ServerLevel) level, blockPos, blockState);
                return ItemInteractionResult.CONSUME;
            }
        } else {
            if (blockEntity.canPlaceItem(itemStack)) {
                // put in note grid
                if (level.isClientSide) {
                    return ItemInteractionResult.SUCCESS;
                }
                blockEntity.setItem(itemStack);
                itemStack.shrink(1);// creative mode also need to shrink
                level.playSound(null, blockPos, SoundEvents.BOOK_PUT, SoundSource.BLOCKS);
                return ItemInteractionResult.CONSUME;
            }
        }

        return super.useItemOn(itemStack, blockState, level, blockPos, player, hand, hitResult);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos blockPos, BlockState blockState, @Nullable LivingEntity livingEntity, ItemStack itemStack) {
        super.setPlacedBy(level, blockPos, blockState, livingEntity, itemStack);
        CustomData customdata = itemStack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
        if (customdata.contains(MusicBoxBlockEntity.NOTE_GRID)) {
            BlockUtils.changeProperty(level, blockPos, blockState, HAS_NOTE_GRID, true, UPDATE_CLIENTS);
        }
    }

    @Override
    public void onRemove(BlockState blockState, Level level, BlockPos blockPos, BlockState blockState1, boolean b) {
        if (!blockState.is(blockState1.getBlock())) {
            if (level.getBlockEntity(blockPos) instanceof MusicBoxBlockEntity blockEntity) {
                Containers.dropItemStack(level, blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockEntity.getItem());
            }
        }
        super.onRemove(blockState, level, blockPos, blockState1, b);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pType) {
        if (pState.getValue(POWERED) && pState.getValue(HAS_NOTE_GRID)) {
            return createTickerHelper(pType, CCMain.MUSIC_BOX_BLOCK_ENTITY.get(), MusicBoxBlockEntity::tick);
        }
        return null;
    }
}
