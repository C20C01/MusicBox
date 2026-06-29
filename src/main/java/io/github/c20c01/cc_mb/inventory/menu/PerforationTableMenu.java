package io.github.c20c01.cc_mb.inventory.menu;

import io.github.c20c01.cc_mb.MusicBox;
import io.github.c20c01.cc_mb.data.NoteGridData;
import io.github.c20c01.cc_mb.inventory.SlotBuilder;
import io.github.c20c01.cc_mb.network.sync.edit.EditDataReceiver;
import io.github.c20c01.cc_mb.player.NoteGridDataHolder;
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

public class PerforationTableMenu extends AbstractContainerMenu implements NoteGridDataHolder {
    public static final byte CODE_EDIT_SCREEN_CLOSE = -1;
    public static final byte CODE_CONNECT_NOTE_GRID = -2;
    public static final byte CODE_PUNCH_FAIL = -3;// player punched at the wrong moment

    private final ContainerLevelAccess access;
    private final Container container = new SimpleContainer(3);
    private final EditDataReceiver receiver;
    private final Slot noteGridSlot;
    private final Slot toolSlot;
    private final Slot otherItemSlot;
    private final Inventory inventory;

    private MenuChangedListener listener;

    @Nullable
    private NoteGridData mainData, helpData;
    private MenuMode mode = MenuMode.EMPTY;

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

    public void setListener(MenuChangedListener listener) {
        this.listener = listener;
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
    public @Nonnull ItemStack quickMoveStack(@Nonnull Player player, int slotIndex) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            clicked = stack.copy();
            if (slotIndex < 3) {
                if (!this.moveItemStackTo(stack, 3, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, 3, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return clicked;
    }

    @Override
    public boolean clickMenuButton(@Nonnull Player player, int code) {
        if (mainData == null) return false;// should not happen

        if (code < 0) {
            switch (code) {
                case CODE_EDIT_SCREEN_CLOSE -> handleEditScreenClose();
                case CODE_CONNECT_NOTE_GRID -> handleConnect();
                case CODE_PUNCH_FAIL -> handlePunchFail();
            }
        } else {
            switch (mode) {
                case CUT -> handleCut(code);
                case PUNCH -> handleEdit((byte) code, true);
                case FIX -> handleEdit((byte) code, false);
            }
        }
        return true;
    }

    private void handleEditScreenClose() {
        assert mainData != null;
        if (receiver.dirty()) {
            mainData.saveToNoteGrid(noteGridSlot.getItem());
            receiver.reset();
            broadcastFullState();
        }
    }

    private void handleConnect() {
        assert mainData != null;
        mainData.saveToNoteGrid(noteGridSlot.getItem());
        access.execute((level, blockPos) -> level.playSound(null, blockPos, SoundEvents.SLIME_BLOCK_FALL, SoundSource.PLAYERS, 1.0F, 1.0F));
        toolSlot.getItem().shrink(1);
        otherItemSlot.getItem().shrink(1);
    }

    private void handlePunchFail() {
        hurtTool(16);
    }

    private void handleCut(int pageNum) {
        assert mainData != null;
        if (pageNum >= mainData.size() - 1 || pageNum < 0) return;// can not cut the last page or invalid page

        ItemStack firstPart = noteGridSlot.getItem();
        ItemStack secondPart = firstPart.copy();

        NoteGridData[] parts = NoteGridUtils.cut(mainData, pageNum + 1);
        parts[0].saveToNoteGrid(firstPart);
        parts[1].saveToNoteGrid(secondPart);

        otherItemSlot.set(secondPart);
        access.execute((level, blockPos) -> level.playSound(null, blockPos, SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 1.0F, 1.0F));
        hurtTool(1);
    }

    private void handleEdit(byte code, boolean add) {
        if (!receiver.receive(code)) return;// buffer is not ready

        if (editNote(receiver.getPageNum(), receiver.getBeatNum(), code, add)) {
            if (add) {
                hurtTool(1);// hurt the awl
            } else {
                toolSlot.getItem().shrink(1);// shrink the paper paste
            }
        }
    }

    private void hurtTool(int damage) {
        access.execute((level, blockPos) ->
                toolSlot.getItem().hurtAndBreak(damage, (ServerLevel) level, inventory.player,
                        _ -> level.playSound(null, blockPos, SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS)));
    }

    private void itemChanged() {
        updateMode();
        updateData();
        if (listener != null) listener.onMenuItemChanged(mainData, helpData, mode);
    }

    /**
     * Update the mode of the table with the current items in the slots.
     */
    private void updateMode() {
        mode = MenuMode.of(noteGridSlot.getItem(), otherItemSlot.getItem(), toolSlot.getItem());
    }

    private void updateData() {
        mainData = NoteGridData.ofItemStack(noteGridSlot.getItem());
        helpData = NoteGridData.ofItemStack(otherItemSlot.getItem());
        if (mode == MenuMode.CONNECT) {
            mainData = NoteGridUtils.connect(mainData, helpData);
        }
    }

    @Override
    public NoteGridData getData() {
        return mainData;
    }

    @Override
    public void setData(NoteGridData data) {
        this.mainData = data;
    }
}
