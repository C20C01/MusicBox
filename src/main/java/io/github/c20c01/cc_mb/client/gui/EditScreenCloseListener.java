package io.github.c20c01.cc_mb.client.gui;

import io.github.c20c01.cc_mb.data.NoteGridData;

public interface EditScreenCloseListener {
    /**
     * Called by {@link NoteGridEditScreen}
     */
    void onEditScreenClose(NoteGridData mainData, int pageNum);
}
