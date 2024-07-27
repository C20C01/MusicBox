package io.github.c20c01.cc_mb.util;

import org.apache.commons.lang3.ArrayUtils;

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

    /**
     * @return true if a contains all elements in b
     */
    public static boolean containsAll(byte[] a, byte[] b) {
        for (byte bb : b) {
            if (!ArrayUtils.contains(a, bb)) {
                return false;
            }
        }
        return true;
    }
}
