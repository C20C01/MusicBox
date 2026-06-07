package io.github.c20c01.cc_mb.player;

public class NoteGridTicker extends NoteGridIterator {
    protected byte tickSinceLastBeat;
    protected byte tickPerBeat = TickPerBeat.DEFAULT;

    public NoteGridTicker(NoteGridDataHolder dataHolder, NoteGridIteratorListener listener) {
        super(dataHolder, listener);
    }

    public byte getTickPerBeat() {
        return tickPerBeat;
    }

    public void setTickPerBeat(int tickPerBeat) {
        this.tickPerBeat = TickPerBeat.clamp(tickPerBeat);
    }

    public void tick() {
        if (++tickSinceLastBeat >= tickPerBeat) {
            tickSinceLastBeat = 0;
            nextBeat();
        }
    }

    public void reset() {
        tickSinceLastBeat = 0;
        super.reset();
    }
}
