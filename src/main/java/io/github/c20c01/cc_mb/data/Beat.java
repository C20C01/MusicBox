package io.github.c20c01.cc_mb.data;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;

import javax.annotation.Nullable;

public final class Beat {
    public static final Beat EMPTY = new Beat();
    private final ByteList notes;
    private byte minNote = Byte.MAX_VALUE;
    private boolean cow = true;

    public Beat() {
        this.notes = new ByteArrayList();
    }

    Beat(ByteList notes) {
        this.notes = new ByteArrayList(notes);
        for (int i = 0; i < notes.size(); i++) {
            byte note = notes.getByte(i);
            if (note < minNote) minNote = note;
        }
    }

    /**
     * Copy constructor (cow -> false)
     */
    private Beat(Beat beat) {
        this.notes = new ByteArrayList(beat.notes);
        this.minNote = beat.minNote;
        this.cow = false;
    }

    static Beat ofCode(String code, int start, int end) {
        if (start >= end) return EMPTY;

        Beat result = new Beat();
        for (int i = start; i < end; i++) {
            byte note = getNoteFromKey(code.charAt(i));
            if (note != -1) {
                result.notes.add(note);
                if (note < result.minNote) result.minNote = note;
            }
        }
        return result;
    }

    /**
     * @param key Key in Note Block Studio, representing a pitch
     * @return 0~24 for a valid key, -1 for an invalid key
     */
    public static byte getNoteFromKey(char key) {
        byte note = -1;
        switch (key) {
            case '1' -> note = 0;
            case 'q' -> note = 1;
            case '2' -> note = 2;
            case 'w' -> note = 3;
            case '3' -> note = 4;
            case 'e' -> note = 5;
            case 'r' -> note = 6;
            case '5' -> note = 7;
            case 't' -> note = 8;
            case '6' -> note = 9;
            case 'y' -> note = 10;
            case 'u' -> note = 11;
            case '8' -> note = 12;
            case 'i' -> note = 13;
            case '9' -> note = 14;
            case 'o' -> note = 15;
            case '0' -> note = 16;
            case 'p' -> note = 17;
            case 'z' -> note = 18;
            case 's' -> note = 19;
            case 'x' -> note = 20;
            case 'd' -> note = 21;
            case 'c' -> note = 22;
            case 'v' -> note = 23;
            case 'g' -> note = 24;
        }
        return note;
    }

    public static boolean isValidNote(byte note) {
        return note <= 24 && note >= 0;
    }

    public ByteList getAddPreviewNotes(byte noteToAdd) {
        int size = notes.size();
        ByteList preview = new ByteArrayList(size + 1);
        boolean shouldAdd = true;
        for (int i = 0; i < size; i++) {
            byte note = notes.getByte(i);
            preview.add(note);
            if (note == noteToAdd) shouldAdd = false;
        }
        if (shouldAdd) preview.add(noteToAdd);
        return preview;
    }

    public ByteList getRemovePreviewNotes(byte noteToRemove) {
        int size = notes.size();
        ByteList preview = new ByteArrayList(size);
        for (int i = 0; i < size; i++) {
            byte note = notes.getByte(i);
            if (note != noteToRemove) preview.add(note);
        }
        return preview;
    }

    public boolean isEmpty() {
        return notes.isEmpty();
    }

    /**
     * Read only!
     */
    public ByteList getNotes() {
        return notes;
    }

    public byte getMinNote() {
        return notes.isEmpty() ? -1 : minNote;
    }

    @Nullable
    Beat withNoteAdded(byte note) {
        if (this.notes.contains(note) || !isValidNote(note)) return null;

        final Beat result = this.cow ? new Beat(this) : this;
        result.notes.add(note);
        if (note < result.minNote) result.minNote = note;
        return result;
    }

    @Nullable
    Beat withNoteRemoved(byte note) {
        int index = this.notes.indexOf(note);
        if (index == -1) return null;

        final Beat result = this.cow ? new Beat(this) : this;
        result.notes.removeByte(index);
        if (note == minNote) {
            result.minNote = Byte.MAX_VALUE;
            for (int i = 0; i < result.notes.size(); i++) {
                byte n = result.notes.getByte(i);
                if (n < result.minNote) result.minNote = n;
            }
        }
        return result;
    }

    Beat withBeatMerged(Beat other) {
        ByteList otherNotes = other.getNotes();
        int size = otherNotes.size();
        if (size == 0) return this;

        boolean cow = this.cow;
        Beat result = this;
        for (int i = 0; i < size; i++) {
            byte note = otherNotes.getByte(i);
            if (result.notes.contains(note)) continue;

            if (cow) {
                cow = false;
                result = new Beat(this);
            }

            result.notes.add(note);
            if (note < result.minNote) result.minNote = note;
        }
        return result;
    }

    public void makeCow() {
        this.cow = true;// Moo~
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Beat beat)) return false;
        return notes.equals(beat.notes);
    }

    @Override
    public int hashCode() {
        return notes.hashCode();
    }
}
