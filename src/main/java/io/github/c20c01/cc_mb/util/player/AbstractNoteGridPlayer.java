package io.github.c20c01.cc_mb.util.player;

import io.github.c20c01.cc_mb.data.Beat;
import io.github.c20c01.cc_mb.data.NoteGridData;
import io.github.c20c01.cc_mb.data.Page;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public abstract class AbstractNoteGridPlayer {
    public static final byte MIN_TICK_PER_BEAT = 1;
    public static final byte MAX_TICK_PER_BEAT = 20;
    private static final Beat EMPTY_BEAT = new Beat();// Read only, used to avoid creating new object
    private static final float[] PITCHES = new float[25];

    static {
        for (int i = 0; i < PITCHES.length; i++) {
            PITCHES[i] = NoteBlock.getPitchFromNote(i);
        }
    }

    protected PlayerListener listener;
    protected byte tickPerBeat = getDefaultTickPerBeat();
    protected byte tickSinceLastBeat;
    protected byte beatNumber;
    protected byte pageNumber;
    protected NoteGridData noteGridData = null;
    private Beat beat = new Beat();

    public AbstractNoteGridPlayer(PlayerListener listener) {
        this.listener = listener;
    }

    public static byte getDefaultTickPerBeat() {
        return (MIN_TICK_PER_BEAT + MAX_TICK_PER_BEAT) / 2;
    }

    /**
     * @param note must be in the range of 0~24, or use {@link NoteBlock#getPitchFromNote(int)}
     */
    public static float getPitchFromNote(byte note) {
        return PITCHES[note];
    }

    protected abstract void playBeat(Level level, BlockPos blockPos, BlockState blockState, Beat beat);

    public void setNoteGridData(@Nullable NoteGridData noteGridData) {
        this.noteGridData = noteGridData;
    }

    public byte getTickPerBeat() {
        return tickPerBeat;
    }

    public void setTickPerBeat(byte tickPerBeat) {
        this.tickPerBeat = (byte) Mth.clamp(tickPerBeat, MIN_TICK_PER_BEAT, MAX_TICK_PER_BEAT);
    }

    /**
     * Called every tick when the player is playing.
     */
    public void tick(Level level, BlockPos blockPos, BlockState blockState) {
        if (++tickSinceLastBeat >= tickPerBeat) {
            nextBeat(level, blockPos, blockState, true);
        }
    }

    /**
     * @param onClient Whether the beat will be played on the client rather than the server.
     */
    public void nextBeat(Level level, BlockPos blockPos, BlockState blockState, boolean onClient) {
        tickSinceLastBeat = 0;
        if (noteGridData == null) {
            return;
        }
        Beat lastBeat = beat;
        try {
            beat = noteGridData.getPage(pageNumber).getBeat(beatNumber, EMPTY_BEAT);
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            listener.onFinish(level, blockPos, blockState);
            reset();
        }
        listener.onBeat(level, blockPos, blockState, lastBeat, beat);
        if (level.isClientSide == onClient) {
            playBeat(level, blockPos, blockState, beat);
        }
        if (++beatNumber >= Page.BEATS_SIZE) {
            nextPage(level, blockPos, blockState);
        }
    }

    private void nextPage(Level level, BlockPos blockPos, BlockState blockState) {
        beatNumber = 0;
        if (++pageNumber >= noteGridData.size()) {
            listener.onFinish(level, blockPos, blockState);
            reset();
        }
        listener.onPageChange(level, blockPos, blockState, pageNumber);
    }

    public void reset() {
        noteGridData = null;
        pageNumber = 0;
        beatNumber = 0;
        tickSinceLastBeat = 0;
        beat = new Beat();
    }

    public byte getMinNote() {
        return beat.getMinNote();
    }
}
