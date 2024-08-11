package io.github.c20c01.cc_mb.util.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.PrimitiveCodec;
import net.minecraft.util.Mth;

public class TickPerBeat {
    public static final byte MIN = 1;
    public static final byte MAX = 20;
    public static final byte DEFAULT = (MIN + MAX) / 2;

    public static byte clamp(int value) {
        return (byte) Mth.clamp(value, MIN, MAX);
    }

    public static final Codec<Byte> CODEC = new PrimitiveCodec<>() {
        @Override
        public <T> DataResult<Byte> read(DynamicOps<T> ops, T input) {
            return ops.getNumberValue(input).map((number) -> (byte) Mth.clamp(number.byteValue(), MIN, MAX));
        }

        @Override
        public <T> T write(DynamicOps<T> ops, Byte value) {
            return ops.createByte(value);
        }
    };
}
