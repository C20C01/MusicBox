package io.github.c20c01.cc_mb.player;

import io.github.c20c01.cc_mb.client.Speaker;
import io.github.c20c01.cc_mb.data.Beat;
import it.unimi.dsi.fastutil.bytes.ByteList;

import javax.annotation.Nullable;

/**
 * Client side only!
 * <p>
 * A player that plays music in the player's mind.
 * Used in the {@link io.github.c20c01.cc_mb.client.gui.NoteGridScreen NoteGridScreen}.
 */
public class MindPlayer implements NoteGridIteratorListener {
    private static byte initialTickPerBeat = TickPerBeat.DEFAULT;// make player can load the last tick per beat setting
    private final NoteGridTicker ticker;
    private final NoteGridDataHolder dataHolder;
    private final NoteGridIteratorListener listener;
    private final SpeakerConfig config;

    public MindPlayer(NoteGridDataHolder dataHolder, NoteGridIteratorListener listener, @Nullable SpeakerConfig config) {
        this.dataHolder = dataHolder;
        this.listener = listener;
        this.config = config != null ? config : SpeakerConfig.MIND_PLAYER_DEFAULT;
        this.ticker = new NoteGridTicker(dataHolder, this);
        this.ticker.setTickPerBeat(initialTickPerBeat);
    }

    public void jumpPageTo(int pageNumber) {
        ticker.pageNum = (byte) Math.max(0, Math.min(pageNumber, dataHolder.getDataSize() - 1));
        ticker.beatNum = 0;
        ticker.tickSinceLastBeat = 0;
    }

    public int getTickToNextBeat() {
        return ticker.tickPerBeat - ticker.tickSinceLastBeat;
    }

    public void skipWaiting() {
        ticker.tickSinceLastBeat = ticker.tickPerBeat;
    }

    public void changeTickPerBeat(int delta) {
        byte newTickPerBeat = (byte) (ticker.tickPerBeat + delta);
        ticker.setTickPerBeat(newTickPerBeat);
        initialTickPerBeat = newTickPerBeat;
    }

    public void tick() {
        ticker.tick();
    }

    public int getBeatNumInInterval(int interval) {
        return (ticker.tickSinceLastBeat + interval) / ticker.tickPerBeat;
    }

    @Override
    public boolean onBeat(int pageNum, int beatNum, Beat beat) {
        boolean shouldContinue = listener.onBeat(pageNum, beatNum, beat);
        if (!shouldContinue) return false;
        if (beat.isEmpty()) return true;
        playNotes(beat.getNotes());
        return true;
    }

    public void playNotes(ByteList notes) {
        Speaker.playMind(config, notes);
    }

    @Override
    public void onPageChanged() {
        listener.onPageChanged();
    }

    @Override
    public void onFinished() {
        listener.onFinished();
    }
}
