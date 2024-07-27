package io.github.c20c01.cc_mb.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class Page {
    public static final byte BEATS_SIZE = 64;
    private final Beat[] BEATS = new Beat[BEATS_SIZE];

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
        return "Page:" + Arrays.toString(BEATS);
    }

    public Beat getBeat(byte index) {
        if (BEATS[index] == null) {
            BEATS[index] = new Beat();
        }
        return BEATS[index];
    }

    /**
     * Read only! If you want to modify the beat, use {@link #getBeat(byte)} instead.
     */
    public Beat getBeat(byte index, Beat defaultBeat) {
        return BEATS[index] == null ? defaultBeat : BEATS[index];
    }

    public boolean isEmptyBeat(byte index) {
        return BEATS[index] == null || BEATS[index].isEmpty();
    }

    public Page setBeats(Beat[] beats) {
        System.arraycopy(beats, 0, BEATS, 0, Math.min(beats.length, BEATS_SIZE));
        return this;
    }

    public Page setBeats(Collection<Beat> beats) {
        return setBeats(beats.toArray(new Beat[0]));
    }
}
