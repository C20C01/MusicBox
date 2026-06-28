package io.github.c20c01.cc_mb.inventory.menu;

import io.github.c20c01.cc_mb.data.NoteGridData;

import javax.annotation.Nullable;

public interface MenuChangedListener {
    /**
     * Called by {@link PerforationTableMenu}.
     */
    void onMenuItemChanged(@Nullable NoteGridData mainData, @Nullable NoteGridData helpData, MenuMode mode);
}
