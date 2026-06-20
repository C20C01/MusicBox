package io.github.c20c01.cc_mb.inventory.menu;

import io.github.c20c01.cc_mb.MusicBox;
import io.github.c20c01.cc_mb.data.NoteGridData;
import io.github.c20c01.cc_mb.inventory.SlotBuilder;
import io.github.c20c01.cc_mb.inventory.menu.edit.EditDataReceiver;
import io.github.c20c01.cc_mb.util.NoteGridUtils;
import net.minecraft.server.level.ServerLevel;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PerforationTableMenu extends AbstractContainerMenu {
    public static final byte CODE_SAVE_NOTE_GRID = -1;
    public static final byte CODE_CONNECT_NOTE_GRID = -2;
    public static final byte CODE_PUNCH_FAIL = -3;// player punched at the wrong moment

    private final ContainerLevelAccess access;
    private final Container container = new SimpleContainer(3);
    private final EditDataReceiver receiver;
    private final Slot noteGridSlot;
    private final Slot toolSlot;
    private final Slot otherItemSlot;
    private final Inventory inventory;
    private Runnable itemChangedCallback;
    private MenuMode mode = MenuMode.EMPTY;
    private NoteGridData data;
    private NoteGridData helpData;
    private NoteGridData displayData;// display result of connect

    public PerforationTableMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    public PerforationTableMenu(int id, Inventory inventory, final ContainerLevelAccess access) {
        super(MusicBox.PERFORATION_TABLE_MENU.get(), id);
        this.access = access;
        this.inventory = inventory;
        this.receiver = new EditDataReceiver();

        this.noteGridSlot = this.addSlot(new SlotBuilder(container, 0, 15, 22)
                .accept(MusicBox.NOTE_GRID_ITEM.get())
                .maxStackSize(1)
                .onChanged(this::itemChanged)
                .build()
        );

        this.toolSlot = this.addSlot(new SlotBuilder(container, 1, 25, 42)
                .accept(MusicBox.PAPER_PASTE_ITEM.get(), MusicBox.AWL_ITEM.get(), Items.SLIME_BALL, Items.SHEARS)
                .maxStackSize(64)
                .onChanged(this::itemChanged)
                .build()
        );

        this.otherItemSlot = this.addSlot(new SlotBuilder(container, 2, 35, 22)
                .accept(MusicBox.NOTE_GRID_ITEM.get(), Items.WRITABLE_BOOK)
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
    public boolean stillValid(@Nonnull Player player) {
        return stillValid(this.access, player, MusicBox.PERFORATION_TABLE_BLOCK.get());
    }

    @Override
    public void removed(@Nonnull Player player) {
        super.removed(player);
        this.access.execute((_, _) -> this.clearContainer(player, container));
    }

    @Override
    public @Nonnull ItemStack quickMoveStack(@Nonnull Player player, int i) {
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
    public boolean clickMenuButton(@Nonnull Player player, int code) {
        if (code < 0) {
            // handle flags
            switch (code) {
                case CODE_SAVE_NOTE_GRID -> {
                    if (receiver.dirty()) {
                        data.saveToNoteGrid(noteGridSlot.getItem());
                        receiver.reset();
                        broadcastFullState();
                    }
                }
                case CODE_CONNECT_NOTE_GRID -> {
                    NoteGridUtils.connect(data, helpData).saveToNoteGrid(noteGridSlot.getItem());
                    access.execute((level, blockPos) -> level.playSound(null, blockPos, SoundEvents.SLIME_BLOCK_FALL, SoundSource.PLAYERS, 1.0F, 1.0F));
                    toolSlot.getItem().shrink(1);
                    otherItemSlot.getItem().shrink(1);
                }
                case CODE_PUNCH_FAIL -> hurtTool(16);
            }
        } else {
            // handle cut
            if (mode == MenuMode.CUT) {
                NoteGridData[] res = NoteGridUtils.cut(data, code + 1);// code == current page
                ItemStack noteGrid = noteGridSlot.getItem();
                ItemStack otherGrid = noteGrid.copy();
                res[0].saveToNoteGrid(noteGrid);
                res[1].saveToNoteGrid(otherGrid);
                otherItemSlot.set(otherGrid);
                access.execute((level, blockPos) -> level.playSound(null, blockPos, SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 1.0F, 1.0F));
                hurtTool(1);
                return true;
            }
            // handle edit
            if (!receiver.receive((byte) code)) {
                return true;
            }
            byte page = receiver.getPageNum();
            byte beat = receiver.getBeatNum();
            if (mode == MenuMode.PUNCH) {
                NoteGridData editedData = data.withNoteChanged(page, beat, (byte) code, true);
                if (data != editedData) {
                    data = editedData;
                    hurtTool(1);
                }
                return true;
            }
            if (mode == MenuMode.FIX) {
                NoteGridData editedData = data.withNoteChanged(page, beat, (byte) code, false);
                if (data != editedData) {
                    data = editedData;
                    toolSlot.getItem().shrink(1);
                }
                return true;
            }
        }
        return true;
    }

    private void hurtTool(int damage) {
        access.execute((level, blockPos) ->
                toolSlot.getItem().hurtAndBreak(damage, (ServerLevel) level, inventory.player,
                        _ -> level.playSound(null, blockPos, SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS)
                )
        );
    }

    protected void itemChanged() {
        updateMode();
        updateData();
        if (itemChangedCallback != null) itemChangedCallback.run();
    }

    /**
     * Update the mode of the table with the current items in the slots.
     */
    private void updateMode() {
        mode = MenuMode.update(noteGridSlot.getItem(), otherItemSlot.getItem(), toolSlot.getItem());
    }

    private void updateData() {
        ItemStack noteGrid = noteGridSlot.getItem();
        this.data = noteGrid.isEmpty() ? null : NoteGridData.ofNoteGrid(noteGrid);
        ItemStack otherItem = otherItemSlot.getItem();
        if (otherItem.isEmpty()) {
            this.helpData = null;
        } else if (otherItem.getItem() == Items.WRITABLE_BOOK) {
            this.helpData = NoteGridData.ofBook(otherItem);
        } else {
            this.helpData = NoteGridData.ofNoteGrid(otherItem);
        }
        if (mode == MenuMode.CONNECT) {
            this.displayData = NoteGridUtils.connect(data, helpData);
        }
    }

    public void setItemChangedCallback(Runnable callback) {
        this.itemChangedCallback = callback;
    }

    public NoteGridData getData() {
        return data;
    }

    public NoteGridData getDisplayData() {
        return displayData;
    }

    @Nullable
    public NoteGridData getHelpData() {
        return helpData;
    }

    public MenuMode getMode() {
        return mode;
    }
}
