package io.github.c20c01.cc_mb.data;

import java.util.Arrays;
import java.util.List;

public final class Page {
    public static final Page EMPTY = new Page();
    public static final byte BEATS_SIZE = 64;
    private final Beat[] beats;
    private boolean cow = true;

    public Page() {
        beats = new Beat[BEATS_SIZE];
        Arrays.fill(beats, Beat.EMPTY);
    }

    Page(List<Beat> beats) {
        this.beats = new Beat[BEATS_SIZE];
        int realSize = Math.min(beats.size(), BEATS_SIZE);
        for (int i = 0; i < realSize; i++) this.beats[i] = beats.get(i);
        Arrays.fill(this.beats, realSize, BEATS_SIZE, Beat.EMPTY);
    }

    /**
     * Copy constructor (cow -> false)
     */
    private Page(Page page) {
        this.beats = new Beat[BEATS_SIZE];
        System.arraycopy(page.beats, 0, this.beats, 0, BEATS_SIZE);
        this.cow = false;
    }

    public static Page ofCode(String code) {
        Page result = new Page();
        if (code.isEmpty()) return result;

        int index = 0, start = 0;
        while (index < BEATS_SIZE) {
            int dotPos = code.indexOf('.', start);
            if (dotPos == -1) {
                result.beats[index] = Beat.ofCode(code, start, code.length());
                break;
            } else {
                result.beats[index] = Beat.ofCode(code, start, dotPos);
                start = dotPos + 1;
                index++;
            }
        }
        return result;
    }

    /**
     * Read only!
     */
    public Beat getBeat(int index) {
        return beats[index];
    }

    Page withBeat(int index, Beat beat) {
        if (this.beats[index] != beat) {
            final Page result = this.cow ? new Page(this) : this;
            result.beats[index] = beat;
            return result;
        }
        return this;
    }

    Page withPageMerged(Page other) {
        boolean cow = this.cow;
        Page result = this;
        for (int i = 0; i < BEATS_SIZE; i++) {
            Beat currentBeat = this.beats[i];
            Beat resultBeat = currentBeat.withBeatMerged(other.beats[i]);
            if (resultBeat == currentBeat) continue;

            if (cow) {
                cow = false;
                result = new Page(this);
            }

            result.beats[i] = resultBeat;
        }
        return result;
    }

    public Page makeCow() {
        if (!cow) {
            for (int i = 0; i < BEATS_SIZE; i++) beats[i].makeCow();
            cow = true;
        }
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Page page)) return false;
        return Arrays.equals(beats, page.beats);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(beats);
    }
}
