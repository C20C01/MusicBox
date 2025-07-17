package io.github.c20c01.cc_mb.block;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.block.entity.SoundBoxBlockEntity;
import io.github.c20c01.cc_mb.util.BlockUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;

public class SoundBoxBlock extends Block implements EntityBlock {
    public static final BooleanProperty HAS_SOUND_SHARD = BooleanProperty.create("has_sound_shard");
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty UNDER_MUSIC_BOX = BooleanProperty.create("under_music_box");

    public SoundBoxBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(HAS_SOUND_SHARD, false)
                .setValue(POWERED, false)
                .setValue(UNDER_MUSIC_BOX, false)
        );
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new SoundBoxBlockEntity(blockPos, blockState);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HAS_SOUND_SHARD, POWERED, UNDER_MUSIC_BOX);
    }

    /**
     * Remove sound shard when the block state is changed to no sound shard.
     * Use this method because the update tag with empty item will not be sent.
     */
    @Override
    public void onBlockStateChange(LevelReader level, BlockPos pos, BlockState oldState, BlockState newState) {
        if (level.getBlockEntity(pos) instanceof SoundBoxBlockEntity blockEntity) {
            if (!newState.getValue(HAS_SOUND_SHARD) && !blockEntity.isEmpty()) {
                blockEntity.removeItem();
            }
        }
        super.onBlockStateChange(level, pos, oldState, newState);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos blockPos = context.getClickedPos();
        boolean underMusicBox = level.getBlockState(blockPos.above()).is(CCMain.MUSIC_BOX_BLOCK.get());
        boolean powered = level.hasNeighborSignal(blockPos) || underMusicBox;
        return this.defaultBlockState().setValue(POWERED, powered).setValue(UNDER_MUSIC_BOX, underMusicBox);
    }

    @Override
    protected BlockState updateShape(BlockState blockState, LevelReader level, ScheduledTickAccess scheduledTickAccess, BlockPos blockPos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (direction != Direction.UP) {
            return super.updateShape(blockState, level, scheduledTickAccess, blockPos, direction, neighborPos, neighborState, random);
        }
        return blockState.setValue(UNDER_MUSIC_BOX, neighborState.is(CCMain.MUSIC_BOX_BLOCK.get()));
    }

    @Override
    public void neighborChanged(BlockState blockState, Level level, BlockPos blockPos, Block block, @Nullable Orientation orientation, boolean isMoving) {
        boolean underMusicBox = blockState.getValue(UNDER_MUSIC_BOX);
        boolean powered = false;

        for (Direction direction : SignalGetter.DIRECTIONS) {
            BlockPos fromPos = blockPos.relative(direction);
            if (!level.hasSignal(fromPos, direction)) {
                continue;
            }
            powered = true;
            if (underMusicBox) {
                continue;
            }
            BlockState fromBlock = level.getBlockState(fromPos);
            if (fromBlock.is(Blocks.LIGHTNING_ROD) && blockPos.relative(fromBlock.getValue(DirectionalBlock.FACING)).equals(fromPos)) {
                // Change sound seed when powered by lightning rod pointing to this block.
                if (SoundBoxBlockEntity.tryToChangeSoundSeed(level, blockPos)) {
                    // show particles
                    level.levelEvent(3002, blockPos, -1);
                }
                break;
            }
        }

        if (!underMusicBox && !blockState.getValue(POWERED) && powered) {
            // play sound
            SoundBoxBlockEntity.tryToPlaySound(level, blockPos);
        }

        level.setBlock(blockPos, blockState.setValue(POWERED, powered), UPDATE_CLIENTS);
    }

    @Override
    public void attack(BlockState blockState, Level level, BlockPos blockPos, Player player) {
        if (!level.getBlockState(blockPos).getValue(UNDER_MUSIC_BOX)) {
            SoundBoxBlockEntity.tryToPlaySound(level, blockPos);
        }
        super.attack(blockState, level, blockPos, player);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack itemStack, BlockState blockState, Level level, BlockPos
            blockPos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        SoundBoxBlockEntity blockEntity = level.getBlockEntity(blockPos) instanceof SoundBoxBlockEntity be ? be : null;
        if (blockEntity == null) {
            return super.useItemOn(itemStack, blockState, level, blockPos, player, hand, hitResult);
        }

        if (blockState.getValue(HAS_SOUND_SHARD)) {
            if (player.isSecondaryUseActive()) {
                // take out sound shard
                if (level.isClientSide) {
                    return InteractionResult.SUCCESS;
                }
                ItemHandlerHelper.giveItemToPlayer(player, blockEntity.removeItem());
                return InteractionResult.CONSUME;
            }
            if (!blockState.getValue(UNDER_MUSIC_BOX)) {
                // play sound
                if (level.isClientSide) {
                    return InteractionResult.SUCCESS;
                }
                SoundBoxBlockEntity.tryToPlaySound(level, blockPos);
                return InteractionResult.CONSUME;
            }
        } else {
            if (itemStack.is(CCMain.SOUND_SHARD_ITEM.get())) {
                if (blockEntity.canPlaceItem(itemStack)) {
                    // put in sound shard
                    if (level.isClientSide) {
                        return InteractionResult.SUCCESS;
                    }
                    blockEntity.setItem(itemStack);
                    itemStack.shrink(1);// creative mode also need to shrink
                    SoundBoxBlockEntity.tryToPlaySound(level, blockPos);
                    return InteractionResult.CONSUME;
                } else {
                    // show message
                    player.displayClientMessage(Component.translatable(CCMain.TEXT_SHARD_WITHOUT_SOUND).withStyle(ChatFormatting.RED), true);
                    return InteractionResult.SUCCESS;
                }
            }
        }

        return super.useItemOn(itemStack, blockState, level, blockPos, player, hand, hitResult);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos blockPos, BlockState blockState, @Nullable LivingEntity
            livingEntity, ItemStack itemStack) {
        super.setPlacedBy(level, blockPos, blockState, livingEntity, itemStack);
        CustomData customdata = itemStack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
        if (customdata.contains(SoundBoxBlockEntity.SOUND_SHARD)) {
            BlockUtils.changeProperty(level, blockPos, blockState, HAS_SOUND_SHARD, true, UPDATE_CLIENTS);
        }
    }
}
