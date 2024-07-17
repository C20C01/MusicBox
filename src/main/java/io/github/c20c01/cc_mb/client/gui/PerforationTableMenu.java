package io.github.c20c01.cc_mb.client.gui;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.data.NoteGridData;
import io.github.c20c01.cc_mb.data.Page;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault

public class PerforationTableMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    private final Container container = new SimpleContainer(3);
    private final Slot noteGridSlot;
    private final Slot toolSlot;
    private final Slot otherGridSlot;
    protected Mode mode = Mode.EMPTY;
    private Page[] pages;
    private boolean isItemChanged = false;

    public PerforationTableMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    public PerforationTableMenu(int id, Inventory inventory, final ContainerLevelAccess access) {
        super(CCMain.PERFORATION_TABLE_MENU.get(), id);
        this.access = access;

        this.noteGridSlot = this.addSlot(new Slot(this.container, 0, 15, 22) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return itemStack.is(CCMain.NOTE_GRID_ITEM.get());
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public void setChanged() {
                super.setChanged();
                itemChanged();
            }
        });

        this.toolSlot = this.addSlot(new Slot(this.container, 1, 25, 42) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return itemStack.is(Items.SLIME_BALL) || itemStack.is(CCMain.AWL_ITEM.get());
            }

            @Override
            public int getMaxStackSize() {
                return 64;
            }

            @Override
            public void setChanged() {
                super.setChanged();
                itemChanged();
            }
        });

        this.otherGridSlot = this.addSlot(new Slot(this.container, 2, 35, 22) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return itemStack.is(CCMain.NOTE_GRID_ITEM.get()) || itemStack.is(Items.WRITABLE_BOOK);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public void setChanged() {
                super.setChanged();
                itemChanged();
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
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, CCMain.PERFORATION_TABLE_BLOCK.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, blockPos) -> this.clearContainer(player, container));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(i);
        if (slot.hasItem()) {
            ItemStack itemStack1 = slot.getItem();
            itemStack = itemStack1.copy();
            if (i < 3) {
                if (!this.moveItemStackTo(itemStack1, 3, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemStack1, 0, 3, false)) {
                return ItemStack.EMPTY;
            }

            if (itemStack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemStack;
    }

    @Override
    public boolean clickMenuButton(Player player, int i) {
        if (i == 0 && mode == Mode.SUPERPOSE) {
            access.execute((level, blockPos) -> superposeGrid(level, blockPos, player));
            return true;
        }
        if (i == 1 && mode == Mode.CONNECT) {
            access.execute(this::connectGrid);
            return true;
        }
        if (i == 2 && mode == Mode.BOOK) {
            access.execute((level, blockPos) -> superposeGridByBook(level, blockPos, player));
            return true;
        }

        return super.clickMenuButton(player, i);
    }

    private void hurtTool(ServerPlayer player, int cost) {
        ItemStack tool = toolSlot.getItem();
        if (!player.getAbilities().instabuild && tool.hurt(cost, player.level().random, player)) {
            tool.shrink(1);
            player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1F, 1F);
        }
    }

    private void superposeGrid(Level level, BlockPos blockPos, Player player) {
        NoteGridData.saveToTag(noteGridSlot.getItem(), NoteGridData.superposeGrid(noteGridSlot.getItem(), otherGridSlot.getItem()));
        afterSuperposeGrid(level, blockPos, player);
    }

    private void connectGrid(Level level, BlockPos blockPos) {
        NoteGridData.saveToTag(noteGridSlot.getItem(), NoteGridData.connectGrid(noteGridSlot.getItem(), otherGridSlot.getItem()));
        otherGridSlot.getItem().shrink(1);
        toolSlot.getItem().shrink(1);
        level.playSound(null, blockPos, SoundEvents.SLIME_BLOCK_FALL, SoundSource.PLAYERS, 1F, 1F);
    }

    private void superposeGridByBook(Level level, BlockPos blockPos, Player player) {
        NoteGridData.saveToTag(noteGridSlot.getItem(), NoteGridData.superposeGridByBook(noteGridSlot.getItem(), otherGridSlot.getItem()));
        afterSuperposeGrid(level, blockPos, player);
    }

    private void afterSuperposeGrid(Level level, BlockPos blockPos, Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            hurtTool(serverPlayer, 10);
            serverPlayer.getInventory().placeItemBackInInventory(container.removeItemNoUpdate(noteGridSlot.getSlotIndex()));
        }
        level.playSound(null, blockPos, SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 1F, 1F);
    }

    public void punchGridOnServer(ServerPlayer player, byte page, byte beat, byte note) {
        punchGrid(page, beat, note);
        hurtTool(player, 1);
    }

    protected boolean punchGrid(byte page, byte beat, byte note) {
        try {
            if (pages[page].getBeat(beat).addOneNote(note)) {
//                NoteGridData.saveToTag(noteGridSlot.getItem(), pages);
                noteGridSlot.setChanged();
                return true;
            }
        } catch (ArrayIndexOutOfBoundsException ignore) {
        }
        return false;
    }

    @Nullable
    protected Page[] getPages() {
        return pages;
    }

    protected boolean shouldUpdate() {
        if (isItemChanged) {
            isItemChanged = false;
            return true;
        } else {
            return false;
        }
    }

    protected void itemChanged() {
        setMode();
        switch (mode) {
            case EMPTY -> pages = null;
            case CHECK, PUNCH -> pages = NoteGridData.readFromTag(noteGridSlot.getItem());
            case SUPERPOSE -> pages = NoteGridData.superposeGrid(noteGridSlot.getItem(), otherGridSlot.getItem());
            case CONNECT -> pages = NoteGridData.connectGrid(noteGridSlot.getItem(), otherGridSlot.getItem());
            case BOOK -> pages = NoteGridData.superposeGridByBook(noteGridSlot.getItem(), otherGridSlot.getItem());
        }
        isItemChanged = true;
    }

    private void setMode() {
        ItemStack noteGrid = noteGridSlot.getItem();
        ItemStack otherGrid = otherGridSlot.getItem();
        ItemStack tool = toolSlot.getItem();

        if (noteGrid.isEmpty()) {
            mode = Mode.EMPTY;
            return;
        }

        if (tool.is(CCMain.AWL_ITEM.get())) {
            ItemStack other = otherGridSlot.getItem();
            if (other.isEmpty()) {
                mode = Mode.PUNCH;
                return;
            }
            if (other.is(CCMain.NOTE_GRID_ITEM.get())) {
                mode = Mode.SUPERPOSE;
            }
            if (other.is(Items.WRITABLE_BOOK)) {
                mode = Mode.BOOK;
                return;
            }
            return;
        }

        if (tool.is(Items.SLIME_BALL) && !otherGrid.isEmpty()) {
            mode = Mode.CONNECT;
            return;
        }

        mode = Mode.CHECK;
    }

    /**
     * EMPTY: 空
     * CHECK: 查看
     * PUNCH: 打孔
     * SUPERPOSE: 叠加
     * CONNECT: 连接
     * BOOK: 从书中读取
     */
    public enum Mode {
        EMPTY, CHECK, PUNCH, SUPERPOSE, CONNECT, BOOK
    }
}
