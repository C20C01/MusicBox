package io.github.c20c01.cc_mb.block;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.block.entity.SoundBoxBlockEntity;
import io.github.c20c01.cc_mb.item.SoundShard;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;

public class SoundBoxBlock extends Block implements EntityBlock {
    public static final BooleanProperty EMPTY = BooleanProperty.create("empty");

    public SoundBoxBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(EMPTY, true));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new SoundBoxBlockEntity(blockPos, blockState);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(EMPTY);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos blockPos, BlockState blockState, @Nullable LivingEntity livingEntity, ItemStack itemStack) {
        super.setPlacedBy(level, blockPos, blockState, livingEntity, itemStack);
        CompoundTag compoundTag = BlockItem.getBlockEntityData(itemStack);
        if (compoundTag != null && compoundTag.contains("SoundShard") && !ItemStack.of(compoundTag.getCompound("SoundShard")).isEmpty()) {
            level.setBlock(blockPos, blockState.setValue(EMPTY, false), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState blockState, Level level, BlockPos blockPos, BlockState blockState1, boolean b) {
        if (!blockState.is(blockState1.getBlock())) {
            if (level.getBlockEntity(blockPos) instanceof SoundBoxBlockEntity blockEntity) {
                Containers.dropItemStack(level, blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockEntity.getFirstItem());
            }
        }
        super.onRemove(blockState, level, blockPos, blockState1, b);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        ItemStack itemStack = player.getItemInHand(hand);
        SoundBoxBlockEntity blockEntity = (SoundBoxBlockEntity) level.getBlockEntity(blockPos);
        if (blockEntity == null) {
            return super.use(blockState, level, blockPos, player, hand, hitResult);
        }

        if (blockState.getValue(EMPTY)) {
            if (itemStack.is(CCMain.SOUND_SHARD_ITEM.get())) {
                // 放入
                if (SoundShard.hasSound(itemStack) && blockEntity.setSoundShard(itemStack)) {
                    itemStack.shrink(1);
                    return InteractionResult.sidedSuccess(level.isClientSide);
                } else {
                    player.displayClientMessage(Component.translatable(CCMain.TEXT_SHARD_WITHOUT_SOUND).withStyle(ChatFormatting.GOLD), true);
                    return InteractionResult.SUCCESS;
                }
            }
        } else {
            if (player.isShiftKeyDown()) {
                // 取出
                ItemStack out = blockEntity.outSoundShard();
                if (!out.isEmpty()) {
                    ItemHandlerHelper.giveItemToPlayer(player, out);
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            } else {
                // 显示声音事件
                if (!level.isClientSide) {
                    player.displayClientMessage(Component.literal(blockEntity.getInstrument().get().getLocation().toString()).withStyle(ChatFormatting.GRAY), true);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }

        return super.use(blockState, level, blockPos, player, hand, hitResult);
    }
}
