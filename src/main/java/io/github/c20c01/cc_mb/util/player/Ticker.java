package io.github.c20c01.cc_mb.util.player;

public class Ticker {
    private final Listener listener;
    protected byte tickSinceLastBeat;
    private byte tickPerBeat = TickPerBeat.DEFAULT;

    public Ticker(Listener listener) {
        this.listener = listener;
    }

    public byte getTickPerBeat() {
        return tickPerBeat;
    }

    public void setTickPerBeat(int tickPerBeat) {
        this.tickPerBeat = TickPerBeat.clamp(tickPerBeat);
    }

    public int tickToNextBeat() {
        return tickPerBeat - tickSinceLastBeat;
    }

    public void skipWaiting() {
        tickSinceLastBeat = tickPerBeat;
    }

    public void tick() {
        if (++tickSinceLastBeat >= tickPerBeat) {
            tickSinceLastBeat = 0;
            listener.nextBeat();
        }
    }

    public interface Listener {
        void nextBeat();
    }
}
