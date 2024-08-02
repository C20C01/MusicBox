package io.github.c20c01.cc_mb.util;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashSet;

public class SlotBuilder {
    private final Container CONTAINER;
    private final int INDEX;
    private final int X;
    private final int Y;
    private final HashSet<Item> ACCEPTED_ITEMS = new HashSet<>();
    private int maxStackSize = 64;
    private Runnable onChanged = () -> {
    };

    public SlotBuilder(Container container, int index, int x, int y) {
        this.CONTAINER = container;
        this.INDEX = index;
        this.X = x;
        this.Y = y;
    }

    public SlotBuilder accept(Item... items) {
        ACCEPTED_ITEMS.addAll(Arrays.asList(items));
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
        return new Slot(CONTAINER, INDEX, X, Y) {
            @Override
            public boolean mayPlace(@Nonnull ItemStack itemStack) {
                return ACCEPTED_ITEMS.contains(itemStack.getItem()) || ACCEPTED_ITEMS.isEmpty();
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
