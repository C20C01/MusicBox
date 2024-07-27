package io.github.c20c01.cc_mb.client.gui;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.util.NoteGridUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public enum MenuMode {
    EMPTY(Component.translatable(CCMain.TEXT_EMPTY)),
    CHECK(Component.translatable(CCMain.TEXT_CHECK)),
    PUNCH(Component.translatable(CCMain.TEXT_PUNCH)),
    CONNECT(Component.translatable(CCMain.TEXT_CONNECT));
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
        if (tool.is(Items.SLIME_BALL) && !otherGrid.isEmpty() && NoteGridUtils.canConnect(noteGrid, otherGrid)) {
            return CONNECT;
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