package io.github.c20c01.cc_mb.data;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteArraySet;

import java.util.Collection;

/**
 * A beat is a set of notes that can be played together.
 */
public class Beat {
    public static final Beat EMPTY_BEAT = new Beat();// Read only
    private final ByteArraySet NOTES = new ByteArraySet();
    private byte minNote = Byte.MAX_VALUE;

    public static Beat ofNotes(Collection<Byte> notes) {
        return new Beat().setNotes(notes);
    }

    public static Beat ofCode(String codeOfBeat) {
        return new Beat().loadCode(codeOfBeat);
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

    public static boolean isAvailableNote(byte note) {
        return note <= 24 && note >= 0;
    }

    public Beat loadCode(String codeOfBeat) {
        if (codeOfBeat.isEmpty()) {
            return this;
        }
        ByteArrayList notes = new ByteArrayList(codeOfBeat.length());
        for (char c : codeOfBeat.toCharArray()) {
            byte note = getNoteFromKey(c);
            if (note != -1) {
                notes.add(note);
            }
        }
        return setNotes(notes);
    }

    @Override
    public String toString() {
        return "Beat:" + NOTES;
    }

    public boolean isEmpty() {
        return NOTES.isEmpty();
    }

    public ByteArraySet getNotes() {
        return NOTES;
    }

    private Beat setNotes(Collection<Byte> notes) {
        NOTES.clear();
        minNote = Byte.MAX_VALUE;
        for (byte note : notes) {
            if (note < minNote) {
                minNote = note;
            }
            NOTES.add(note);
        }
        return this;
    }

    public byte getMinNote() {
        return NOTES.isEmpty() ? -1 : minNote;
    }

    public boolean addNote(byte note) {
        if (isAvailableNote(note) && NOTES.add(note)) {
            if (note < minNote) {
                minNote = note;
            }
            return true;
        }
        return false;
    }

    public boolean removeNote(byte note) {
        if (NOTES.remove(note)) {
            if (note == minNote) {
                minNote = Byte.MAX_VALUE;
                for (byte n : NOTES) {
                    if (n < minNote) {
                        minNote = n;
                    }
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return NOTES.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Beat beat) {
            return NOTES.containsAll(beat.NOTES);
        }
        return super.equals(obj);
    }
}
