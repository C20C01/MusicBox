package io.github.c20c01.cc_mb.client.gui;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.util.NoteGridUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public enum MenuMode {
    EMPTY(Component.translatable(CCMain.TEXT_EMPTY)),
    CHECK(Component.translatable(CCMain.TEXT_CHECK)),
    PUNCH(Component.translatable(CCMain.TEXT_PUNCH)),
    CONNECT(Component.translatable(CCMain.TEXT_CONNECT)),
    FIX(Component.translatable(CCMain.TEXT_FIX));
    private final Component TIP;

    MenuMode(Component tip) {
        this.TIP = tip;
    }

    public static MenuMode update(ItemStack noteGrid, ItemStack otherGrid, ItemStack tool) {
        if (noteGrid.isEmpty()) {
            return EMPTY;
        }
        if (tool.isEmpty()) {
            return CHECK;
        }
        if (tool.is(CCMain.AWL_ITEM.get())) {
            return PUNCH;
        }
        if (tool.is(CCMain.PAPER_PASTE_ITEM) && otherGrid.is(CCMain.NOTE_GRID_ITEM.get()) && NoteGridUtils.canConnect(noteGrid, otherGrid)) {
            return CONNECT;
        }
        if (tool.is(CCMain.PAPER_PASTE_ITEM)) {
            return FIX;
        }
        return CHECK;
    }

    /**
     * @return The component displayed over the {@link NoteGridWidget widget}.
     */
    public Component getTip() {
        return TIP;
    }
}