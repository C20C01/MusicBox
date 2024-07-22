package io.github.c20c01.cc_mb.util;

import java.util.Collection;

public class CollectionUtils {
    public static byte[] toArray(Collection<Byte> c) {
        byte[] array = new byte[c.size()];
        int i = 0;
        for (Byte b : c) {
            array[i++] = b == null ? -1 : b;
        }
        return array;
    }
}
