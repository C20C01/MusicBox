package io.github.c20c01.cc_mb.player;


import io.github.c20c01.cc_mb.data.Beat;

public class NoteGridIterator {
    protected final NoteGridDataHolder dataHolder;
    protected final NoteGridIteratorListener listener;
    protected byte beatNum;
    protected byte pageNum;
    protected Beat currentBeat = Beat.EMPTY;

    public NoteGridIterator(NoteGridDataHolder dataHolder, NoteGridIteratorListener listener) {
        this.dataHolder = dataHolder;
        this.listener = listener;
    }

    /**
     * @return whether the note grid has finished playing
     */
    protected boolean nextBeat() {
        if (beatNum >= 64 && nextPage()) return true;
        currentBeat = dataHolder.getBeat(pageNum, beatNum);
        if (listener.onBeat(currentBeat, beatNum)) beatNum++;
        return false;
    }

    private boolean nextPage() {
        beatNum = 0;
        if (++pageNum >= dataHolder.getDataSize()) {
            listener.onFinish();
            reset();
            return true;
        }
        listener.onPageChanged(pageNum);
        return false;
    }

    public void reset() {
        pageNum = 0;
        beatNum = 0;
    }
}
