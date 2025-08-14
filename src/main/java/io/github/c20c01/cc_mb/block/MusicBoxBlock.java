package io.github.c20c01.cc_mb.block;

import com.mojang.serialization.MapCodec;
import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.block.entity.MusicBoxBlockEntity;
import io.github.c20c01.cc_mb.item.Awl;
import io.github.c20c01.cc_mb.util.player.TickPerBeat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class MusicBoxBlock extends BaseBoxPlayerBlock {
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
    public BlockState updateShape(BlockState blockState, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos blockPos, BlockPos neighborPos) {
        return direction == Direction.DOWN ? this.setInstrument(level, blockPos, blockState) : super.updateShape(blockState, direction, neighborState, level, blockPos, neighborPos);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos blockPos = context.getClickedPos();
        return this.setInstrument(level, blockPos, super.getStateForPlacement(context));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(INSTRUMENT);
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
                return takeOutNoteGrid(level, blockEntity, player);
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
                return putInNoteGrid(level, blockPos, blockEntity, itemStack);
            }
        }

        return super.useItemOn(itemStack, blockState, level, blockPos, player, hand, hitResult);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new MusicBoxBlockEntity(blockPos, blockState);
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
