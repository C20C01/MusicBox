package io.github.c20c01.cc_mb.client.gui;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.data.NoteGridData;
import io.github.c20c01.cc_mb.util.NoteGridUtils;
import io.github.c20c01.cc_mb.util.SlotBuilder;
import io.github.c20c01.cc_mb.util.punch.PunchDataReceiver;
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

import javax.annotation.Nonnull;

public class PerforationTableMenu extends AbstractContainerMenu {
    public static final byte CODE_SAVE_NOTE_GRID = -1;
    public static final byte CODE_CONNECT_NOTE_GRID = -2;
    public static final byte CODE_PUNCH_FAIL = -3;// player punched at the wrong moment

    private final ContainerLevelAccess ACCESS;
    private final Container CONTAINER = new SimpleContainer(3);
    private final PunchDataReceiver PUNCH_DATA_RECEIVER;
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
        super(CCMain.PERFORATION_TABLE_MENU.get(), id);
        this.ACCESS = access;
        this.INVENTORY = inventory;
        this.PUNCH_DATA_RECEIVER = new PunchDataReceiver(() -> data);

        this.NOTE_GRID_SLOT = this.addSlot(new SlotBuilder(CONTAINER, 0, 15, 22)
                .accept(CCMain.NOTE_GRID_ITEM.get())
                .maxStackSize(1)
                .onChanged(this::itemChanged)
                .build()
        );

        this.TOOL_SLOT = this.addSlot(new SlotBuilder(CONTAINER, 1, 25, 42)
                .accept(Items.SLIME_BALL, CCMain.AWL_ITEM.get())
                .maxStackSize(64)
                .onChanged(this::itemChanged)
                .build()
        );

        this.OTHER_GRID_SLOT = this.addSlot(new SlotBuilder(CONTAINER, 2, 35, 22)
                .accept(CCMain.NOTE_GRID_ITEM.get(), Items.WRITABLE_BOOK)
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
        return stillValid(this.ACCESS, player, CCMain.PERFORATION_TABLE_BLOCK.get());
    }

    @Override
    public void removed(@Nonnull Player player) {
        super.removed(player);
        this.ACCESS.execute((level, blockPos) -> this.clearContainer(player, CONTAINER));
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
            // handel flags
            switch (code) {
                case CODE_SAVE_NOTE_GRID -> {
                    data.saveToNoteGrid(NOTE_GRID_SLOT.getItem());
                    PUNCH_DATA_RECEIVER.reset();
                    broadcastFullState();
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
            // handle punch
            if (PUNCH_DATA_RECEIVER.receive((byte) code)) {
                hurtTool(1);
            }
        }
        return true;
    }

    private void hurtTool(int damage) {
        ItemStack tool = TOOL_SLOT.getItem();
        Player player = INVENTORY.player;
        if (player.getAbilities().instabuild) {
            return;
        }
        if (tool.hurt(damage, player.getRandom(), (ServerPlayer) player)) {
            tool.shrink(1);
            ACCESS.execute((level, blockPos) -> level.playSound(null, blockPos, SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F));
        }
    }

    protected void itemChanged() {
        updateMode();
        updateData();
        if (screen != null) {
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
