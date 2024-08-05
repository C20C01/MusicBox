package io.github.c20c01.cc_mb.data;

import io.github.c20c01.cc_mb.CCMain;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;

public class PresetNoteGridData {
    private final ArrayList<ItemStack> NOTE_GRIDS = new ArrayList<>();

    public PresetNoteGridData() {
        add(Component.literal("Little Star").withStyle(ChatFormatting.GOLD), NoteGridData.ofBytes(
                new byte[]{
                        -1, 7, -1, 7, -1, 14, -1, 14, -1, 16, -1, 16, -1, 14,
                        -2, 12, -1, 12, -1, 11, -1, 11, -1, 9, -1, 9, -1, 7,
                        -2, 14, -1, 14, -1, 12, -1, 12, -1, 11, -1, 11, -1, 9,
                        -2, 14, -1, 14, -1, 12, -1, 12, -1, 11, -1, 11, -1, 9,
                        -2, 7, -1, 7, -1, 14, -1, 14, -1, 16, -1, 16, -1, 14,
                        -2, 12, -1, 12, -1, 11, -1, 11, -1, 9, -1, 9, -1, 7, 0
                }
        ));
        addNoteGridWithSize(2);
        addNoteGridWithSize(4);
        addNoteGridWithSize(8);
        addNoteGridWithSize(16);
        addNoteGridWithSize(32);
        addNoteGridWithSize(64);
    }

    private void addNoteGridWithSize(int size) {
        Component name = Component.translatable(CCMain.NOTE_GRID_ITEM.get().getDescriptionId()).append("(" + size + ")");
        add(name, NoteGridData.ofPages(new Page[size]));
    }

    public ArrayList<ItemStack> getItems() {
        return NOTE_GRIDS;
    }

    private void add(Component name, NoteGridData data) {
        ItemStack itemStack = data.saveToNoteGrid(new ItemStack(CCMain.NOTE_GRID_ITEM.get()));
        itemStack.set(DataComponents.ITEM_NAME, name);
        NOTE_GRIDS.add(itemStack);
    }
}