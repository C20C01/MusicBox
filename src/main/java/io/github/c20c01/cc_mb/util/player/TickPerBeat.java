package io.github.c20c01.cc_mb.util.player;

import net.minecraft.util.Mth;

public class TickPerBeat {
    public static final byte MIN = 1;
    public static final byte MAX = 20;
    public static final byte DEFAULT = (MIN + MAX) / 2;

    public static byte clamp(int value) {
        return (byte) Mth.clamp(value, MIN, MAX);
    }
}
