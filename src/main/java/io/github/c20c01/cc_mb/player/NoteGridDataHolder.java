package io.github.c20c01.cc_mb.player;

import io.github.c20c01.cc_mb.data.Beat;
import io.github.c20c01.cc_mb.data.NoteGridData;

import javax.annotation.Nullable;

public interface NoteGridDataHolder {
    @Nullable
    NoteGridData getData();

    void setData(@Nullable NoteGridData data);

    int getDataSize();

    Beat getBeat(int pageNum, int beatNum);
}
