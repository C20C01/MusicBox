package io.github.c20c01.cc_mb.block;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.block.entity.SoundBoxBlockEntity;
import io.github.c20c01.cc_mb.util.BlockUtils;
import io.github.c20c01.cc_mb.util.ItemUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

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

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos blockPos = context.getClickedPos();
        boolean underMusicBox = level.getBlockState(blockPos.above()).is(CCMain.MUSIC_BOX_BLOCK);
        boolean powered = level.hasNeighborSignal(blockPos) || underMusicBox;
        return this.defaultBlockState().setValue(POWERED, powered).setValue(UNDER_MUSIC_BOX, underMusicBox);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(BlockState blockState, Level level, BlockPos blockPos, Block block, BlockPos fromPos, boolean isMoving) {
        boolean underMusicBox = level.getBlockState(blockPos.above()).is(CCMain.MUSIC_BOX_BLOCK);
        boolean powered = level.hasNeighborSignal(blockPos) || underMusicBox;
        if (!underMusicBox && powered) {
            BlockState fromBlock = level.getBlockState(fromPos);
            if (fromBlock.is(Blocks.LIGHTNING_ROD) && fromBlock.getValue(BlockStateProperties.POWERED) && blockPos.relative(fromBlock.getValue(DirectionalBlock.FACING)).equals(fromPos)) {
                // Change sound seed when powered by lightning rod pointing to this block.
                if (SoundBoxBlockEntity.tryToChangeSoundSeed(level, blockPos)) {
                    // show particles
                    level.levelEvent(3002, blockPos, -1);
                }
            }
            if (!blockState.getValue(POWERED)) {
                // play sound
                SoundBoxBlockEntity.tryToPlaySound(level, blockPos);
            }
        }
        level.setBlock(blockPos, blockState.setValue(POWERED, powered).setValue(UNDER_MUSIC_BOX, underMusicBox), UPDATE_CLIENTS);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void attack(BlockState blockState, Level level, BlockPos blockPos, Player player) {
        if (!level.getBlockState(blockPos).getValue(UNDER_MUSIC_BOX)) {
            SoundBoxBlockEntity.tryToPlaySound(level, blockPos);
        }
        super.attack(blockState, level, blockPos, player);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        ItemStack itemStack = player.getItemInHand(hand);
        SoundBoxBlockEntity blockEntity = level.getBlockEntity(blockPos) instanceof SoundBoxBlockEntity be ? be : null;
        if (blockEntity == null) {
            return super.use(blockState, level, blockPos, player, hand, hitResult);
        }

        if (blockState.getValue(HAS_SOUND_SHARD)) {
            if (player.isSecondaryUseActive()) {
                // take out sound shard
                if (level.isClientSide) {
                    return InteractionResult.SUCCESS;
                }
                ItemUtils.give(player, blockEntity.removeItem());
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
            if (itemStack.is(CCMain.SOUND_SHARD_ITEM)) {
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
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            }
        }

        return super.use(blockState, level, blockPos, player, hand, hitResult);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos blockPos, BlockState blockState, @Nullable LivingEntity livingEntity, ItemStack itemStack) {
        super.setPlacedBy(level, blockPos, blockState, livingEntity, itemStack);
        CompoundTag compoundTag = BlockItem.getBlockEntityData(itemStack);
        if (compoundTag != null && compoundTag.contains(SoundBoxBlockEntity.SOUND_SHARD)) {
            BlockUtils.changeProperty(level, blockPos, blockState, HAS_SOUND_SHARD, true, UPDATE_CLIENTS);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState blockState, Level level, BlockPos blockPos, BlockState blockState1, boolean b) {
        if (!blockState.is(blockState1.getBlock())) {
            if (level.getBlockEntity(blockPos) instanceof SoundBoxBlockEntity blockEntity) {
                Containers.dropItemStack(level, blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockEntity.getItem());
            }
        }
        super.onRemove(blockState, level, blockPos, blockState1, b);
    }
}
