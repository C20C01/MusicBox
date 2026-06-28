package io.github.c20c01.cc_mb.client.gui;

import io.github.c20c01.cc_mb.inventory.menu.MenuMode;

public interface MenuModeChangedListener {
    /**
     * Called by {@link PerforationTableScreen}.
     */
    void onMenuModeChanged(MenuMode mode);
}
