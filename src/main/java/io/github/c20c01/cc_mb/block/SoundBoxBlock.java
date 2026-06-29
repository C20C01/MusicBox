package io.github.c20c01.cc_mb.block;

import io.github.c20c01.cc_mb.MusicBox;
import io.github.c20c01.cc_mb.block.entity.SoundBoxBlockEntity;
import io.github.c20c01.cc_mb.util.BlockUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class SoundBoxBlock extends Block implements EntityBlock {
    public static final BooleanProperty HAS_SOUND_SHARD = BooleanProperty.create("has_sound_shard");
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public SoundBoxBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(HAS_SOUND_SHARD, false)
                .setValue(POWERED, false)
        );
    }

    private static boolean notUnderMusicBox(LevelReader level, BlockPos blockPos) {
        return !level.getBlockState(blockPos.above()).is(MusicBox.MUSIC_BOX_BLOCK.get());
    }

    private static void tryToPlaySound(Level level, BlockPos blockPos) {
        if (!level.isClientSide() && level.getBlockEntity(blockPos) instanceof SoundBoxBlockEntity soundBox) {
            soundBox.playSound(level, blockPos);
        }
    }

    private static InteractionResult tryToPlaySound(Level level, BlockPos blockPos, SoundBoxBlockEntity soundBox) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        soundBox.playSound(level, blockPos);
        return InteractionResult.CONSUME;
    }

    private static InteractionResult tryToPutInSoundShard(Level level, BlockPos blockPos, Player player, SoundBoxBlockEntity soundBox, ItemStack soundShard) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (soundBox.canPlaceItem(soundShard)) {
            soundBox.setItem(soundShard.copy());
            soundBox.playSound(level, blockPos);
            soundShard.shrink(1);// creative mode also need to shrink
            return InteractionResult.CONSUME;
        } else {
            player.sendOverlayMessage(Component.translatable(MusicBox.TEXT_SHARD_WITHOUT_SOUND).withStyle(ChatFormatting.RED));
            return InteractionResult.SUCCESS;
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new SoundBoxBlockEntity(blockPos, blockState);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HAS_SOUND_SHARD, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (state.getValue(HAS_SOUND_SHARD)
                && neighbourState.getBlock() instanceof LightningRodBlock
                && neighbourState.getValue(LightningRodBlock.POWERED)
                && directionToNeighbour == neighbourState.getValue(DirectionalBlock.FACING)) {
            // Try to change sound seed when lightning rod that pointing this block is powered by lightning.
            if (SoundBoxBlockEntity.tryToChangeSoundSeed((Level) level, pos)) {
                // Remove oxidation from copper blocks.
                ((Level) level).levelEvent(3002, pos, -1);
            }
        }

        boolean hasSignal = level.hasNeighborSignal(pos);
        if (state.getValue(POWERED) != hasSignal) {
            if (hasSignal && notUnderMusicBox(level, pos)) tryToPlaySound((Level) level, pos);
            return state.setValue(POWERED, hasSignal);
        }

        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    public void attack(BlockState blockState, Level level, BlockPos blockPos, Player player) {
        if (notUnderMusicBox(level, blockPos)) tryToPlaySound(level, blockPos);
        super.attack(blockState, level, blockPos, player);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack itemStack, BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(blockPos) instanceof SoundBoxBlockEntity soundBox)) {
            return super.useItemOn(itemStack, blockState, level, blockPos, player, hand, hitResult);
        }

        if (blockState.getValue(HAS_SOUND_SHARD)) {
            if (player.isSecondaryUseActive()) {
                return SingleItemContainerBlock.takeOutItem(level, soundBox, player.getInventory());
            }
            if (notUnderMusicBox(level, blockPos)) {
                return tryToPlaySound(level, blockPos, soundBox);
            }
        } else {
            if (itemStack.is(MusicBox.SOUND_SHARD_ITEM.get())) {
                return tryToPutInSoundShard(level, blockPos, player, soundBox, itemStack);
            }
        }

        return super.useItemOn(itemStack, blockState, level, blockPos, player, hand, hitResult);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos blockPos, BlockState blockState, @Nullable LivingEntity livingEntity, ItemStack itemStack) {
        super.setPlacedBy(level, blockPos, blockState, livingEntity, itemStack);
        if (level.getBlockEntity(blockPos) instanceof SoundBoxBlockEntity soundBox) {
            if (!soundBox.isEmpty()) {
                BlockUtils.changeProperty(level, blockPos, blockState, HAS_SOUND_SHARD, true, UPDATE_CLIENTS);
            }
        }
    }
}
