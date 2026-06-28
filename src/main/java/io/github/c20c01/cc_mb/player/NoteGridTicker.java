package io.github.c20c01.cc_mb.player;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class NoteGridTicker extends NoteGridIterator {
    protected byte tickSinceLastBeat;
    protected byte tickPerBeat = TickPerBeat.DEFAULT;

    public NoteGridTicker(NoteGridDataHolder dataHolder, NoteGridIteratorListener listener) {
        super(dataHolder, listener);
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

    public void loadAdditional(ValueInput input) {
        setTickPerBeat(input.getByteOr("tick_per_beat", tickPerBeat));
        tickSinceLastBeat = input.getByteOr("interval", tickSinceLastBeat);
        super.loadAdditional(input);
    }

    public void saveAdditional(ValueOutput output) {
        output.putByte("tick_per_beat", tickPerBeat);
        output.putByte("interval", tickSinceLastBeat);
        super.saveAdditional(output);
    }
}
