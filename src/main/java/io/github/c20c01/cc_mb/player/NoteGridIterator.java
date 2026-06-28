package io.github.c20c01.cc_mb.player;

import io.github.c20c01.cc_mb.data.Beat;
import io.github.c20c01.cc_mb.data.Page;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class NoteGridIterator {
    protected final NoteGridDataHolder dataHolder;
    protected final NoteGridIteratorListener listener;
    protected byte pageNum;
    protected byte beatNum;
    protected Beat currentBeat = Beat.EMPTY;

    public NoteGridIterator(NoteGridDataHolder dataHolder, NoteGridIteratorListener listener) {
        this.dataHolder = dataHolder;
        this.listener = listener;
    }

    public void nextBeat() {
        if (beatNum >= Page.BEATS_SIZE && nextPage()) return;
        currentBeat = dataHolder.getBeat(pageNum, beatNum);
        if (listener.onBeat(pageNum, beatNum, currentBeat)) beatNum++;
    }

    private boolean nextPage() {
        beatNum = 0;
        if (++pageNum >= dataHolder.getDataSize()) {
            reset();
            listener.onFinished();
            return true;
        }
        listener.onPageChanged();
        return false;
    }

    public byte getMinNote() {
        return currentBeat.getMinNote();
    }

    public void reset() {
        pageNum = 0;
        beatNum = 0;
        currentBeat = Beat.EMPTY;
    }

    public void loadAdditional(ValueInput input) {
        beatNum = input.getByteOr("beat", beatNum);
        pageNum = input.getByteOr("page", pageNum);
    }

    public void saveAdditional(ValueOutput output) {
        output.putByte("beat", beatNum);
        output.putByte("page", pageNum);
    }
}
