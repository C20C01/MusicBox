package io.github.c20c01.cc_mb.util.player;

import io.github.c20c01.cc_mb.data.Beat;
import io.github.c20c01.cc_mb.data.NoteGridData;
import io.github.c20c01.cc_mb.data.Page;

import javax.annotation.Nullable;

public abstract class BasePlayer implements Ticker.Listener {
    @Nullable
    public NoteGridData data = null;
    protected Beat currentBeat = Beat.EMPTY_BEAT;
    protected byte beatNumber;
    protected byte pageNumber;

    protected abstract void onPageChange();

    protected abstract void onFinish();

    protected abstract boolean shouldPause();

    protected abstract void playBeat();

    public void nextBeat() {
        if (beatNumber >= Page.BEATS_SIZE && nextPage()) {
            return;
        }
        updateCurrentBeat();
        if (shouldPause()) {
            return;
        }
        playBeat();
        beatNumber++;
    }

    public void updateCurrentBeat() {
        if (data != null && pageNumber < data.size()) {
            currentBeat = data.getPage(pageNumber).readBeat(beatNumber);
        } else {
            reset();
        }
    }

    private boolean nextPage() {
        beatNumber = 0;
        if (data != null && ++pageNumber < data.size()) {
            onPageChange();
            return false;
        } else {
            onFinish();
            reset();
            return true;
        }
    }

    public void reset() {
        pageNumber = 0;
        beatNumber = 0;
        currentBeat = Beat.EMPTY_BEAT;
        data = null;
    }
}
