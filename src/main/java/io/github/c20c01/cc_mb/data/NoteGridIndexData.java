package io.github.c20c01.cc_mb.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

public class NoteGridIndexData extends SavedData {
    public static final String KEY = "NoteGridIndex";
    private int nextId = 0;

    public static NoteGridIndexData load(CompoundTag pCompoundTag) {
        NoteGridIndexData indexData = new NoteGridIndexData();
        indexData.nextId = pCompoundTag.getInt("nextId");
        return indexData;
    }

    @Override
    public CompoundTag save(CompoundTag pCompound) {
        pCompound.putInt("nextId", nextId);
        return pCompound;
    }

    public int getNextId() {
        setDirty();
        return nextId++;
    }
}
