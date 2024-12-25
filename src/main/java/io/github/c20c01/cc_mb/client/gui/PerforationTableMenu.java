package io.github.c20c01.cc_mb.client.gui;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.data.Beat;
import io.github.c20c01.cc_mb.data.NoteGridData;
import io.github.c20c01.cc_mb.util.NoteGridUtils;
import io.github.c20c01.cc_mb.util.SlotBuilder;
import io.github.c20c01.cc_mb.util.edit.EditDataReceiver;
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
import org.jetbrains.annotations.NotNull;

public class PerforationTableMenu extends AbstractContainerMenu {
    public static final byte CODE_SAVE_NOTE_GRID = -1;
    public static final byte CODE_CONNECT_NOTE_GRID = -2;
    public static final byte CODE_PUNCH_FAIL = -3;// player punched at the wrong moment

    private final ContainerLevelAccess ACCESS;
    private final Container CONTAINER = new SimpleContainer(3);
    private final EditDataReceiver EDIT_DATA_RECEIVER;
    private final Slot NOTE_GRID_SLOT;
    private final Slot TOOL_SLOT;
    private final Slot OTHER_GRID_SLOT;
    private final Inventory INVENTORY;
    protected PerforationTableScreen screen;
    protected MenuMode mode = MenuMode.EMPTY;
    protected NoteGridData data;
    protected NoteGridData helpData;
    protected NoteGridData displayData;// display result of connect

    public PerforationTableMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    public PerforationTableMenu(int id, Inventory inventory, final ContainerLevelAccess access) {
        super(CCMain.PERFORATION_TABLE_MENU, id);
        this.ACCESS = access;
        this.INVENTORY = inventory;
        this.EDIT_DATA_RECEIVER = new EditDataReceiver();

        this.NOTE_GRID_SLOT = this.addSlot(new SlotBuilder(CONTAINER, 0, 15, 22)
                .accept(CCMain.NOTE_GRID_ITEM)
                .maxStackSize(1)
                .onChanged(this::itemChanged)
                .build()
        );

        this.TOOL_SLOT = this.addSlot(new SlotBuilder(CONTAINER, 1, 25, 42)
                .accept(CCMain.PAPER_PASTE_ITEM, CCMain.AWL_ITEM, Items.SLIME_BALL)
                .maxStackSize(64)
                .onChanged(this::itemChanged)
                .build()
        );

        this.OTHER_GRID_SLOT = this.addSlot(new SlotBuilder(CONTAINER, 2, 35, 22)
                .accept(CCMain.NOTE_GRID_ITEM, Items.WRITABLE_BOOK)
                .maxStackSize(1)
                .onChanged(this::itemChanged)
                .build()
        );

        this.addPlayerSlots(inventory);
    }

    private void addPlayerSlots(Inventory inventory) {
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
        return stillValid(this.ACCESS, player, CCMain.PERFORATION_TABLE_BLOCK);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.ACCESS.execute((level, blockPos) -> this.clearContainer(player, CONTAINER));
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int i) {
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
    public boolean clickMenuButton(Player player, int code) {
        if (code < 0) {
            // handel flags
            switch (code) {
                case CODE_SAVE_NOTE_GRID -> {
                    if (EDIT_DATA_RECEIVER.dirty()) {
                        data.saveToNoteGrid(NOTE_GRID_SLOT.getItem());
                        EDIT_DATA_RECEIVER.reset();
                        broadcastFullState();
                    }
                }
                case CODE_CONNECT_NOTE_GRID -> {
                    NoteGridUtils.connect(data, helpData).saveToNoteGrid(NOTE_GRID_SLOT.getItem());
                    ACCESS.execute((level, blockPos) -> level.playSound(null, blockPos, SoundEvents.SLIME_BLOCK_FALL, SoundSource.PLAYERS, 1.0F, 1.0F));
                    TOOL_SLOT.getItem().shrink(1);
                    OTHER_GRID_SLOT.getItem().shrink(1);
                }
                case CODE_PUNCH_FAIL -> hurtTool(16);
            }
        } else {
            // handle edit
            if (!EDIT_DATA_RECEIVER.receive((byte) code)) {
                return true;
            }
            Beat beat = EDIT_DATA_RECEIVER.getBeat(data);
            if (mode == MenuMode.PUNCH) {
                if (beat.addNote((byte) code)) {
                    hurtTool(1);
                }
                return true;
            }
            if (mode == MenuMode.FIX) {
                if (beat.removeNote((byte) code)) {
                    TOOL_SLOT.getItem().shrink(1);
                }
                return true;
            }
        }
        return true;
    }

    private void hurtTool(int damage) {
        ACCESS.execute((level, blockPos) ->
                TOOL_SLOT.getItem().hurtAndBreak(damage, INVENTORY.player,
                        player -> level.playSound(null, blockPos, SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F)
                )
        );
    }

    protected void itemChanged() {
        updateMode();
        updateData();
        if (screen != null) {
            // FIXME
            screen.onItemChanged();
        }
    }

    /**
     * Update the mode of the table with the current items in the slots.
     */
    private void updateMode() {
        final ItemStack NOTE_GRID = NOTE_GRID_SLOT.getItem();
        final ItemStack OTHER_GRID = OTHER_GRID_SLOT.getItem();
        final ItemStack TOOL = TOOL_SLOT.getItem();
        mode = MenuMode.update(NOTE_GRID, OTHER_GRID, TOOL);
    }

    private void updateData() {
        final ItemStack NOTE_GRID = NOTE_GRID_SLOT.getItem();
        this.data = NOTE_GRID.isEmpty() ? null : NoteGridData.ofNoteGrid(NOTE_GRID);
        final ItemStack OTHER_GRID = OTHER_GRID_SLOT.getItem();
        if (OTHER_GRID.isEmpty()) {
            this.helpData = null;
        } else if (OTHER_GRID.getItem() == Items.WRITABLE_BOOK) {
            this.helpData = NoteGridData.ofBook(OTHER_GRID);
        } else {
            this.helpData = NoteGridData.ofNoteGrid(OTHER_GRID);
        }
        if (mode == MenuMode.CONNECT) {
            this.displayData = NoteGridUtils.connect(data.deepCopy(), helpData);
        }
    }
}
