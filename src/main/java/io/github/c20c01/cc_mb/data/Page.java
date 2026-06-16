package io.github.c20c01.cc_mb.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public final class Page {
    public static final byte BEATS_SIZE = 64;
    private final Beat[] beats = new Beat[BEATS_SIZE];

    public static Page ofBeats(Collection<Beat> beats) {
        return new Page().loadBeats(beats);
    }

    public static Page ofCode(String codeOfPage) {
        return new Page().loadCode(codeOfPage);
    }

    public Page loadBeats(Collection<Beat> beats) {
        return setBeats(beats);
    }

    public Page loadCode(String codeOfPage) {
        String[] codesOfBeat = codeOfPage.split("\\.");
        ArrayList<Beat> beats = new ArrayList<>(codesOfBeat.length);
        for (String codeOfBeat : codesOfBeat) {
            beats.add(Beat.ofCode(codeOfBeat));
        }
        return setBeats(beats);
    }

    @Override
    public String toString() {
        return "Page:" + Arrays.toString(beats);
    }

    public Beat getBeat(byte index) {
        if (beats[index] == null) {
            beats[index] = new Beat();
        }
        return beats[index];
    }

    /**
     * Read only! If you want to modify the beat, use {@link #getBeat(byte)} instead.
     */
    public Beat readBeat(byte index) {
        return beats[index] == null ? Beat.EMPTY_BEAT : beats[index];
    }

    public boolean isEmptyBeat(byte index) {
        return beats[index] == null || beats[index].isEmpty();
    }

    public Page setBeats(Beat[] beats) {
        System.arraycopy(beats, 0, this.beats, 0, Math.min(beats.length, BEATS_SIZE));
        return this;
    }

    public Page setBeats(Collection<Beat> beats) {
        return setBeats(beats.toArray(new Beat[0]));
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(beats);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Page page) {
            return Arrays.equals(beats, page.beats);
        }
        return false;
    }
}
