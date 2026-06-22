package io.github.c20c01.cc_mb.block;

import com.mojang.serialization.MapCodec;
import io.github.c20c01.cc_mb.MusicBox;
import io.github.c20c01.cc_mb.block.entity.MusicBoxBlockEntity;
import io.github.c20c01.cc_mb.item.Awl;
import io.github.c20c01.cc_mb.player.TickPerBeat;
import io.github.c20c01.cc_mb.util.BlockUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class MusicBoxBlock extends BaseEntityBlock implements NoteGridBoxBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final MapCodec<MusicBoxBlock> CODEC = simpleCodec(MusicBoxBlock::new);

    public MusicBoxBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HAS_NOTE_GRID, false)
                .setValue(POWERED, false)
        );
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected BlockState updateShape(BlockState blockState, LevelReader level, ScheduledTickAccess scheduledTickAccess, BlockPos blockPos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (direction == Direction.DOWN && level.getBlockEntity(blockPos) instanceof MusicBoxBlockEntity musicBox) {
            musicBox.updateInstrumentFromBelow((Level) level, blockPos.below());
        }
        return super.updateShape(blockState, level, scheduledTickAccess, blockPos, direction, neighborPos, neighborState, random);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    protected void neighborChanged(BlockState blockState, Level level, BlockPos blockPos, Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston) {
        BlockUtils.changeProperty(level, blockPos, blockState, POWERED, level.hasNeighborSignal(blockPos), UPDATE_CLIENTS);
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
        builder.add(FACING, HAS_NOTE_GRID, POWERED);
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
     * For creative mode setting the octave, see {@link Awl#canDestroyBlock}
     */
    @Override
    public void attack(BlockState blockState, Level level, BlockPos blockPos, Player player) {
        if (level.isClientSide() || !(level.getBlockEntity(blockPos) instanceof MusicBoxBlockEntity musicBox)) {
            super.attack(blockState, level, blockPos, player);
            return;
        }
        if (player.getItemInHand(InteractionHand.MAIN_HAND).is(MusicBox.AWL_ITEM)) {
            musicBox.cycleOctave(level, player);
            return;
        }
        if (blockState.getValue(HAS_NOTE_GRID) && !blockState.getValue(POWERED)) {
            musicBox.playNextBeat(level);
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack itemStack, BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(blockPos) instanceof MusicBoxBlockEntity musicBox)) {
            return super.useItemOn(itemStack, blockState, level, blockPos, player, hand, hitResult);
        }

        if (itemStack.is(MusicBox.AWL_ITEM.get()) && !player.isSecondaryUseActive()) {
            return modifyTickPerBeat(level, blockPos, player, itemStack, musicBox);
        }

        if (blockState.getValue(HAS_NOTE_GRID)) {
            if (player.isSecondaryUseActive()) {
                return takeOutNoteGrid(level, musicBox, player.getInventory());
            }
            if (!blockState.getValue(POWERED)) {
                if (player.getAbilities().instabuild && itemStack.is(Items.WRITABLE_BOOK) || itemStack.is(MusicBox.NOTE_GRID_ITEM.get())) {
                    return createNoteGridMerged(level, blockPos, blockState, itemStack, musicBox);
                }
                return playNextBeat(level, musicBox);
            }
        } else {
            return putInNoteGrid(level, blockPos, musicBox, itemStack);
        }

        return super.useItemOn(itemStack, blockState, level, blockPos, player, hand, hitResult);
    }

    private InteractionResult modifyTickPerBeat(Level level, BlockPos blockPos, Player player, ItemStack awl, MusicBoxBlockEntity musicBox) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        musicBox.setTickPerBeat(awl.getOrDefault(MusicBox.TICK_PER_BEAT, TickPerBeat.DEFAULT));
        level.playSound(null, blockPos, SoundEvents.SPYGLASS_USE, SoundSource.PLAYERS);
        player.sendOverlayMessage(Component.translatable(MusicBox.TEXT_CHANGE_TICK_PER_BEAT).append(String.valueOf(musicBox.getTickPerBeat())).withStyle(ChatFormatting.DARK_AQUA));
        return InteractionResult.CONSUME;
    }

    private InteractionResult createNoteGridMerged(Level level, BlockPos blockPos, BlockState blockState, ItemStack itemStack, MusicBoxBlockEntity musicBox) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        ItemStack created = musicBox.createNoteGridMerged(itemStack);
        if (created != null) {
            musicBox.ejectNoteGrid(level, blockPos, blockState, created);
            level.playSound(null, blockPos, SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return InteractionResult.CONSUME;
    }

    private InteractionResult playNextBeat(Level level, MusicBoxBlockEntity musicBox) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        musicBox.playNextBeat(level);
        return InteractionResult.CONSUME;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos blockPos, BlockState blockState, @Nullable LivingEntity livingEntity, ItemStack itemStack) {
        super.setPlacedBy(level, blockPos, blockState, livingEntity, itemStack);
        if (level.getBlockEntity(blockPos) instanceof MusicBoxBlockEntity musicBox) {
            musicBox.updateInstrumentFromBelow(level, blockPos.below());
            if (!musicBox.isEmpty()) {
                BlockUtils.changeProperty(level, blockPos, blockState, HAS_NOTE_GRID, true, UPDATE_CLIENTS);
            }
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (state.getValue(POWERED) && state.getValue(HAS_NOTE_GRID)) {
            return createTickerHelper(type, MusicBox.MUSIC_BOX_BLOCK_ENTITY.get(), MusicBoxBlockEntity::tick);
        }
        return null;
    }
}
