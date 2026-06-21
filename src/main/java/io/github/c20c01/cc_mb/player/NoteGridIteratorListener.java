package io.github.c20c01.cc_mb.player;

import io.github.c20c01.cc_mb.data.Beat;

public interface NoteGridIteratorListener {
    /**
     * @param pageNum the current page number.
     * @param beatNum the current beat number.
     * @param beat    the current beat to be played, READ ONLY!
     * @return whether the player should move to the next beat.
     */
    boolean onBeat(int pageNum, int beatNum, Beat beat);

    /**
     * @param pageNum the new page number.
     */
    void onPageChanged(int pageNum);

    void onFinish();
}
