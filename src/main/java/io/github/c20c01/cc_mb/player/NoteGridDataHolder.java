package io.github.c20c01.cc_mb.player;

import io.github.c20c01.cc_mb.data.Beat;
import io.github.c20c01.cc_mb.data.NoteGridData;

import javax.annotation.Nullable;

public interface NoteGridDataHolder {
    @Nullable
    NoteGridData getData();

    void setData(@Nullable NoteGridData data);

    default boolean hasData() {
        return getData() != null;
    }

    default int getDataSize() {
        NoteGridData data = getData();
        return data == null ? 0 : data.size();
    }

    default Beat getBeat(int pageNum, int beatNum) {
        NoteGridData data = getData();
        if (data == null || data.size() <= pageNum) {
            return Beat.EMPTY;
        } else {
            return data.getPage(pageNum).getBeat(beatNum);
        }
    }

    default boolean editNote(int pageNum, int beatNum, byte note, boolean add) {
        NoteGridData data = getData();
        if (data == null) return false;

        NoteGridData edited = data.withNoteChanged(pageNum, beatNum, note, add);
        if (edited == null) return false;

        setData(edited);
        return true;
    }
}
