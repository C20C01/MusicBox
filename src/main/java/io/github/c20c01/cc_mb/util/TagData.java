package io.github.c20c01.cc_mb.util;

import net.minecraft.nbt.Tag;

public interface TagData<T extends Tag> {
    TagData<T> loadTag(T tag);

    T toTag();
}
