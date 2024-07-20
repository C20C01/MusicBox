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
    public AbstractItemLoaderBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState, String itemTag) {
        super(pType, pPos, pBlockState);
        ITEM_TAG = itemTag;
    }

    abstract protected void loadItem(ItemStack item);

    abstract protected void unloadItem();

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        if (pTag.contains(ITEM_TAG)) {
            setItem(ItemStack.of(pTag.getCompound(ITEM_TAG)));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        super.saveAdditional(pTag);
        if (!item.isEmpty()) {
            pTag.put(ITEM_TAG, item.save(new CompoundTag()));
        }
    }

    public ItemStack getItem() {
        return item;
    }

    /**
     * Set the item in the container then {@link #loadItem(ItemStack) load} it.
     *
     * @param item The item to set, will be copied.
     */
    public boolean setItem(ItemStack item) {
        return handleSetItem(item.copy());
    }

    /**
     * Remove the item from the container then {@link #unloadItem() unload} it.
     */
    public ItemStack removeItem() {
        return removeItem(0, 1);
    }

    private boolean handleSetItem(ItemStack pStack) {
        if (canPlaceItem(pStack)) {
            item = pStack;
            loadItem(item);
            return true;
        }
        return false;
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
        if (item.isEmpty() || pSlot != 0) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = item.copy();
        unloadItem();
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

    public boolean canPlaceItem(ItemStack pStack) {
        return canPlaceItem(0, pStack);
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return Container.stillValidBlockEntity(this, pPlayer);
    }
}
