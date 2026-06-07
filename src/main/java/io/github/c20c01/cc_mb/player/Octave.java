package io.github.c20c01.cc_mb.player;

public class Octave {
    public static final byte MAX = 2;
    public static final byte MIN = -2;

    public static byte clamp(byte octave) {
        return (byte) Math.max(MIN, Math.min(MAX, octave));
    }
}
