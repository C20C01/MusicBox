package io.github.c20c01.cc_mb.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

@SuppressWarnings("NullableProblems")
public interface SingleItemContainer extends Container {

    ItemStack getItem();

    /**
     * @param itemStack use copy if you need use the itemStack after calling this method
     */
    void setItem(final ItemStack itemStack);

    default ItemStack removeItem() {
        ItemStack itemBeforeRemoving = getItem();
        setItem(ItemStack.EMPTY);
        return itemBeforeRemoving;
    }

    @Override
    default int getMaxStackSize() {
        return 1;
    }

    @Override
    default int getContainerSize() {
        return 1;
    }

    @Override
    default boolean isEmpty() {
        return getItem().isEmpty();
    }

    @Override
    default ItemStack getItem(int slot) {
        return slot == 0 ? getItem() : ItemStack.EMPTY;
    }

    @Override
    default void setItem(int slot, ItemStack itemStack) {
        if (slot == 0) setItem(itemStack);
    }

    @Override
    default ItemStack removeItem(int slot, int count) {
        return slot == 0 ? removeItem() : ItemStack.EMPTY;
    }

    @Override
    default ItemStack removeItemNoUpdate(int slot) {
        return slot == 0 ? removeItem() : ItemStack.EMPTY;
    }

    @Override
    default void clearContent() {
        setItem(ItemStack.EMPTY);
    }

    interface SingleItemContainerBlockEntity extends SingleItemContainer {
        BlockEntity getContainerBlockEntity();

        default boolean stillValid(Player player) {
            return Container.stillValidBlockEntity(this.getContainerBlockEntity(), player);
        }
    }
}
