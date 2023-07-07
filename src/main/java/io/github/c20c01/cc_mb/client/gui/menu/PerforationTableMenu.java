package io.github.c20c01.cc_mb.client.gui.menu;

import io.github.c20c01.cc_mb.CCMain;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault

public class PerforationTableMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    private final Container inputContainer = new SimpleContainer(2);
    private final Slot noteGridSlot;
    private final Slot otherSlot;

    public PerforationTableMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    public PerforationTableMenu(int id, Inventory inventory, final ContainerLevelAccess access) {
        super(CCMain.PERFORATION_TABLE_MENU.get(), id);
        this.access = access;

        this.noteGridSlot = this.addSlot(new Slot(this.inputContainer, 0, 13, 26) {
            public boolean mayPlace(ItemStack itemStack) {
                return itemStack.is(CCMain.NOTE_GRID_ITEM.get());
            }
        });

        this.otherSlot = this.addSlot(new Slot(this.inputContainer, 1, 33, 26) {
            public boolean mayPlace(ItemStack itemStack) {
                return itemStack.getItem() instanceof DyeItem;
            }
        });

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(inventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(inventory, k, 8 + k * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int p_38942_) {
        CCMain.LOGGER.info(player.toString());
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, CCMain.PERFORATION_TABLE_BLOCK.get());
    }
}
