package io.github.c20c01.cc_mb.block.entity;

import io.github.c20c01.cc_mb.inventory.SingleItemContainer;
import io.github.c20c01.cc_mb.util.BlockUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public abstract class SingleItemContainerBlockEntityImpl extends BlockEntity implements SingleItemContainer.SingleItemContainerBlockEntity {
    protected final String itemKey;
    protected final BooleanProperty hasItemProperty;

    private ItemStack item = ItemStack.EMPTY;

    public SingleItemContainerBlockEntityImpl(BlockEntityType<?> type, BlockPos blockPos, BlockState blockState, String itemKey, BooleanProperty hasItemProperty) {
        super(type, blockPos, blockState);
        this.itemKey = itemKey;
        this.hasItemProperty = hasItemProperty;
    }

    abstract public boolean canPlaceItem(ItemStack itemStack);

    abstract public boolean canTakeItem(Container target, int index, ItemStack itemStack);

    @Override
    protected void loadAdditional(ValueInput input) {
        input.read(itemKey, ItemStack.CODEC).ifPresent(this::setItem);
        super.loadAdditional(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        if (!item.isEmpty()) output.store(itemKey, ItemStack.CODEC, getItem());
        super.saveAdditional(output);
    }

    @Override
    public BlockEntity getContainerBlockEntity() {
        return this;
    }

    @Override
    public ItemStack getItem() {
        return item;
    }

    @Override
    public void setItem(ItemStack itemStack) {
        this.item = itemStack;
        if (level != null) {
            BlockUtils.changeProperty(level, worldPosition, getBlockState(), hasItemProperty, !itemStack.isEmpty());
            setChanged(level, worldPosition, getBlockState());
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack itemStack) {
        return slot == 0 && canPlaceItem(itemStack);
    }
}
