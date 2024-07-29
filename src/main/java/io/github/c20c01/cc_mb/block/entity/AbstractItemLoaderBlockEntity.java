package io.github.c20c01.cc_mb.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block entity that can hold a single item,
 * with the ability to {@link #loadItem(ItemStack) load} and {@link #unloadItem() unload} it.
 */
public abstract class AbstractItemLoaderBlockEntity extends BlockEntity implements Container {
    private final String ITEM_TAG;
    private ItemStack item = ItemStack.EMPTY;

    /**
     * @param itemTag The key to use when saving the item to NBT.
     */
    public AbstractItemLoaderBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState, String itemTag) {
        super(blockEntityType, blockPos, blockState);
        ITEM_TAG = itemTag;
    }

    /**
     * Triggered when the item is set by {@link #setItem(ItemStack)}.
     * {@link #setChanged()} will be called after this method.
     * <p>
     * Usually used to load data from the item and change the block state.
     */
    abstract protected void loadItem(ItemStack itemStack);

    /**
     * Triggered when the item is removed by {@link #removeItem()}.
     * {@link #setChanged()} will be called after this method.
     * <p>
     * Usually used to unload data and change the block state.
     */
    abstract protected void unloadItem();

    /**
     * Check if the item can be placed in the container.
     * <p>
     * Empty is checked before this method is called.
     */
    abstract public boolean canPlaceItem(ItemStack pStack);

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (!tag.contains(ITEM_TAG)) {
            return;
        }
        ItemStack itemStack = ItemStack.of(tag.getCompound(ITEM_TAG));
        if (itemStack.isEmpty()) {
            if (!isEmpty()) {
                removeItem();
            }
        } else {
            setItem(itemStack);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        // EMPTY will be saved too for the sync
        tag.put(ITEM_TAG, item.save(new CompoundTag()));
    }

    public ItemStack getItem() {
        return item;
    }

    /**
     * Set the item in the container then {@link #loadItem(ItemStack) load} it.
     *
     * @param itemStack The item to set, will be copied.
     */
    public void setItem(ItemStack itemStack) {
        handleSetItem(itemStack.copy());
    }

    /**
     * Remove the item from the container then {@link #unloadItem() unload} it.
     */
    public ItemStack removeItem() {
        return removeItem(0, 1);
    }

    private void handleSetItem(ItemStack itemStack) {
        if (canPlaceItem(0, itemStack)) {
            item = itemStack;
            loadItem(item);
            setChanged();
        }
    }

    @Override
    public final int getContainerSize() {
        return 1;
    }

    @Override
    public final int getMaxStackSize() {
        return 1;
    }

    @Override
    public void clearContent() {
        this.removeItemNoUpdate(0);
    }

    @Override
    public boolean isEmpty() {
        return item.isEmpty();
    }

    @Override
    public ItemStack getItem(int pSlot) {
        return pSlot == 0 ? item : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int pSlot, int pAmount) {
        if (pSlot != 0) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = item.copy();
        unloadItem();
        setChanged();
        item = ItemStack.EMPTY;
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int pSlot) {
        return this.removeItem(pSlot, 1);
    }

    @Override
    public boolean canTakeItem(Container pTarget, int pIndex, ItemStack pStack) {
        return false;
    }

    @Override
    public void setItem(int pSlot, ItemStack pStack) {
        if (pSlot == 0) {
            handleSetItem(pStack);
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack itemStack) {
        return isEmpty() && canPlaceItem(itemStack);
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return Container.stillValidBlockEntity(this, pPlayer);
    }
}
