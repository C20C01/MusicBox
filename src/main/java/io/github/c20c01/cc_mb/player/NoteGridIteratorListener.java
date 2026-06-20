package io.github.c20c01.cc_mb.player;

import io.github.c20c01.cc_mb.data.Beat;

public interface NoteGridIteratorListener {
    /**
     * @param beat       the current beat to play.
     * @param beatNumber the current beat number in the page, from 0 to 63.
     * @return whether the player should move to the next beat.
     */
    boolean onBeat(Beat beat, int beatNumber);

    void onPageChanged(int pageNum);

    void onFinish();
}
