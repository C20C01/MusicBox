package io.github.c20c01.cc_mb.inventory.menu;

import io.github.c20c01.cc_mb.MusicBox;
import io.github.c20c01.cc_mb.util.NoteGridUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public enum MenuMode {
    EMPTY(Component.translatable(MusicBox.TEXT_EMPTY)),
    CHECK(Component.translatable(MusicBox.TEXT_CHECK)),
    PUNCH(Component.translatable(MusicBox.TEXT_PUNCH)),
    FIX(Component.translatable(MusicBox.TEXT_FIX)),
    CONNECT(Component.translatable(MusicBox.TEXT_CONNECT)),
    CUT(Component.translatable(MusicBox.TEXT_CUT));

    public final Component message;

    MenuMode(Component message) {
        this.message = message;
    }

    public static MenuMode of(ItemStack noteGrid, ItemStack otherGrid, ItemStack tool) {
        if (noteGrid.isEmpty()) {
            return EMPTY;
        }
        if (tool.isEmpty()) {
            return CHECK;
        }
        if (tool.is(MusicBox.AWL_ITEM.get())) {
            return PUNCH;
        }
        if (tool.is(MusicBox.PAPER_PASTE_ITEM.get())) {
            return FIX;
        }
        if (tool.is(Items.SHEARS) && otherGrid.isEmpty()) {
            return CUT;
        }
        if (tool.is(Items.SLIME_BALL) && otherGrid.is(MusicBox.NOTE_GRID_ITEM.get()) && NoteGridUtils.canConnect(noteGrid, otherGrid)) {
            return CONNECT;
        }
        return CHECK;
    }
}