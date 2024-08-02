package io.github.c20c01.cc_mb.util.player;

import io.github.c20c01.cc_mb.data.Beat;
import io.github.c20c01.cc_mb.data.Page;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.NoteBlock;

public abstract class AbstractNoteGridPlayer {
    private static final float[] PITCHES = new float[25];

    static {
        for (int i = 0; i < PITCHES.length; i++) {
            PITCHES[i] = NoteBlock.getPitchFromNote(i);
        }
    }

    protected byte tickSinceLastBeat;
    protected byte beatNumber;
    protected byte pageNumber;
    protected Beat currentBeat = Beat.EMPTY_BEAT;
    protected long seed;
    protected Holder<SoundEvent> sound;
    private byte tickPerBeat = TickPerBeat.DEFAULT;

    /**
     * @param note must be in the range of 0~24, or use {@link NoteBlock#getPitchFromNote(int)}
     */
    public static float getPitchFromNote(byte note) {
        return PITCHES[note];
    }

    protected abstract void playBeat();

    protected abstract void updateCurrentBeat();

    protected abstract byte dataSize();

    /**
     * @return Whether the player should pause
     */
    protected abstract boolean onBeat();

    protected abstract void onPageChange();

    protected abstract void onFinish();

    public byte getTickPerBeat() {
        return tickPerBeat;
    }

    public void setTickPerBeat(int tickPerBeat) {
        this.tickPerBeat = TickPerBeat.clamp(tickPerBeat);
    }

    public void tick() {
        if (++tickSinceLastBeat >= tickPerBeat) {
            nextBeat();
        }
    }

    protected void nextBeat() {
        tickSinceLastBeat = 0;
        if (beatNumber >= Page.BEATS_SIZE && nextPage()) {
            return;
        }
        updateCurrentBeat();
        if (onBeat()) {
            return;
        }
        playBeat();
        beatNumber++;
    }

    private boolean nextPage() {
        beatNumber = 0;
        if (++pageNumber >= dataSize()) {
            onFinish();
            reset();
            return true;
        }
        onPageChange();
        return false;
    }

    public void reset() {
        pageNumber = 0;
        beatNumber = 0;
        tickSinceLastBeat = 0;
        currentBeat = Beat.EMPTY_BEAT;
    }
}
