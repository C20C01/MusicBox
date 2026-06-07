package io.github.c20c01.cc_mb.player;

import io.github.c20c01.cc_mb.data.Beat;
import io.github.c20c01.cc_mb.data.NoteGridData;

import javax.annotation.Nullable;

public interface NoteGridDataHolder {
    @Nullable
    NoteGridData getData();

    void setData(@Nullable NoteGridData data);

    byte getDataSize();

    /**
     * @return the beat at the given page and beat number for updating the current beat.
     */
    Beat getBeat(byte pageNum, byte beatNum);
}
