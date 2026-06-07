package io.github.c20c01.cc_mb.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashSet;

public class SlotBuilder {
    private final Container container;
    private final int index;
    private final int x;
    private final int y;
    private final HashSet<Item> acceptedItems = new HashSet<>();
    private int maxStackSize = 64;
    private Runnable onChanged = () -> {
    };

    public SlotBuilder(Container container, int index, int x, int y) {
        this.container = container;
        this.index = index;
        this.x = x;
        this.y = y;
    }

    public SlotBuilder accept(Item... items) {
        acceptedItems.addAll(Arrays.asList(items));
        return this;
    }

    public SlotBuilder maxStackSize(int maxStackSize) {
        this.maxStackSize = maxStackSize;
        return this;
    }

    public SlotBuilder onChanged(Runnable onChanged) {
        this.onChanged = onChanged;
        return this;
    }

    public Slot build() {
        return new Slot(container, index, x, y) {
            @Override
            public boolean mayPlace(@Nonnull ItemStack itemStack) {
                return acceptedItems.contains(itemStack.getItem()) || acceptedItems.isEmpty();
            }

            @Override
            public int getMaxStackSize() {
                return maxStackSize;
            }

            @Override
            public void setChanged() {
                super.setChanged();
                onChanged.run();
            }
        };
    }
}
