package io.github.c20c01.cc_mb.data;

import io.github.c20c01.cc_mb.CCMain;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;

public class PresetNoteGridData {
    private static final ArrayList<ItemStack> NOTE_GRIDS = new ArrayList<>();

    private static void updateNoteGrids() {
        for (int size = 2; size <= 64; size *= 2) {
            Component name = Component.translatable(CCMain.NOTE_GRID_ITEM.getDescriptionId()).append("(" + size + ")");
            add(name, NoteGridData.ofPages(new Page[size]));
        }
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
    }

    private static void add(Component name, NoteGridData data) {
        NOTE_GRIDS.add(data.toNoteGrid().setHoverName(name));
    }

    public static ArrayList<ItemStack> getItems() {
        if (NOTE_GRIDS.isEmpty()) {
            updateNoteGrids();
        }
        return NOTE_GRIDS;
    }
}