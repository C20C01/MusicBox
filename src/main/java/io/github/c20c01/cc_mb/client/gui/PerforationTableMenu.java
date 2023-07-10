package io.github.c20c01.cc_mb.client.gui;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.item.NoteGrid;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
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
    private NoteGrid.Page[] pages;
    private boolean isItemChanged = false;
    private final Slot noteGridSlot;
    private final Slot toolSlot;
    private final Slot otherGridSlot;
    protected Mode mode = Mode.PUNCH;

    protected enum Mode {
        EMPTY, CHECK, PUNCH, CLONE, CONNECT
    }

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
                return itemStack.is(Items.SLIME_BALL) || itemStack.is(CCMain.PUNCHER_ITEM.get());
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

        this.otherGridSlot = this.addSlot(new Slot(this.container, 2, 35, 22) {
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
        if (i == 0 && mode == Mode.CLONE) {
            access.execute((level, blockPos) -> cloneGrid(level, blockPos, player));
            return true;
        }
        if (i == 1 && mode == Mode.CONNECT) {
            access.execute(this::connectGrid);
            return true;
        }

        return super.clickMenuButton(player, i);
    }

    private void hurtTool(Player player, int cost) {
        if (player instanceof ServerPlayer) {
            ItemStack tool = toolSlot.getItem();
            if (!player.getAbilities().instabuild && tool.hurt(cost, player.level().random, (ServerPlayer) player)) {
                tool.shrink(1);
                player.playSound(SoundEvents.ANVIL_BREAK);
            }
        }
    }

    private void cloneGrid(Level level, BlockPos blockPos, Player player) {
        NoteGrid.Page[] pages = NoteGrid.readFromTag(noteGridSlot.getItem());
        int otherPagesLen = NoteGrid.readFromTag(otherGridSlot.getItem()).length;
        SoundEvent sound;
        if (pages.length == otherPagesLen) {
            NoteGrid.saveToTag(otherGridSlot.getItem(), pages);
            hurtTool(player, pages.length * 4);
            sound = SoundEvents.ANVIL_USE;
        } else {
            sound = SoundEvents.VILLAGER_NO;
        }
        Inventory inventory = player.getInventory();
        if (inventory.player instanceof ServerPlayer) {
            inventory.placeItemBackInInventory(container.removeItemNoUpdate(2));
        }
        level.playSound(null, blockPos, sound, SoundSource.PLAYERS, 1F, 1F);
    }

    private void connectGrid(Level level, BlockPos blockPos) {
        NoteGrid.saveToTag(noteGridSlot.getItem(), NoteGrid.connect(noteGridSlot.getItem(), otherGridSlot.getItem()));
        otherGridSlot.getItem().shrink(1);
        toolSlot.getItem().shrink(1);
        level.playSound(null, blockPos, SoundEvents.SLIME_BLOCK_FALL, SoundSource.PLAYERS, 1F, 1F);
    }

    public void punchGridOnServer(ServerPlayer player, byte page, byte beat, byte note) {
        hurtTool(player, 2);
        punchGrid(page, beat, note);
    }

    protected boolean punchGrid(byte page, byte beat, byte note) {
        try {
            if (pages[page].getBeat(beat).addNote(note)) {
                NoteGrid.saveToTag(noteGridSlot.getItem(), pages);
                noteGridSlot.setChanged();
                return true;
            }
        } catch (ArrayIndexOutOfBoundsException ignore) {
        }
        return false;
    }

    @Nullable
    protected NoteGrid.Page[] getPages() {
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
            case CHECK, CLONE, PUNCH -> pages = NoteGrid.readFromTag(noteGridSlot.getItem());
            case CONNECT -> pages = NoteGrid.connect(noteGridSlot.getItem(), otherGridSlot.getItem());
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

        if (tool.is(CCMain.PUNCHER_ITEM.get())) {
            if (otherGridSlot.getItem().isEmpty()) {
                mode = Mode.PUNCH;
            } else {
                mode = Mode.CLONE;
            }
            return;
        }

        if (tool.is(Items.SLIME_BALL) && !otherGrid.isEmpty()) {
            mode = Mode.CONNECT;
            return;
        }

        mode = Mode.CHECK;
    }
}
