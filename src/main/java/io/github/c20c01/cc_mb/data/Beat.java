package io.github.c20c01.cc_mb.data;

import io.github.c20c01.cc_mb.util.TagData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NoteBlock;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * A beat is a set of notes that can be played together.
 */
public class Beat implements TagData<ByteArrayTag> {
    private byte[] notes = ArrayUtils.EMPTY_BYTE_ARRAY;
    private byte minNote = Byte.MAX_VALUE;

    public static Beat ofNotes(byte[] notes) {
        return new Beat().loadNotes(notes);
    }

    public static Beat ofCode(String codeOfBeat) {
        return new Beat().loadCode(codeOfBeat);
    }

    public static Beat ofTag(ByteArrayTag beatTag) {
        return new Beat().loadTag(beatTag);
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

    public Beat loadNotes(byte[] notes) {
        return setNotes(notes);
    }

    public Beat loadCode(String codeOfBeat) {
        if (codeOfBeat.isEmpty()) {
            return this;
        }
        Set<Byte> notes = new HashSet<>(codeOfBeat.length());
        for (char c : codeOfBeat.toCharArray()) {
            byte note = getNoteFromKey(c);
            if (note != -1) {
                notes.add(note);
            }
        }
        return setNotes(notes);
    }

    @Override
    public Beat loadTag(ByteArrayTag beatTag) {
        byte[] notes = beatTag.getAsByteArray();
        return loadNotes(notes);
    }

    @Override
    public ByteArrayTag toTag() {
        return new ByteArrayTag(notes);
    }

    @Override
    public String toString() {
        return "Beat:" + Arrays.toString(notes);
    }

    public boolean isEmpty() {
        return notes.length == 0;
    }

    public byte[] getNotes() {
        return notes;
    }

    public Beat setNotes(byte[] newNotes) {
        minNote = Byte.MAX_VALUE;
        if (newNotes.length == 0) {
            notes = ArrayUtils.EMPTY_BYTE_ARRAY;
            return this;
        }
        notes = newNotes;
        for (byte note : notes) {
            if (note < minNote) {
                minNote = note;
            }
        }
        return this;
    }

    public Beat setNotes(Collection<Byte> notes) {
        byte[] notesArray = ArrayUtils.toPrimitive(notes.toArray(new Byte[0]));
        return setNotes(notesArray);
    }

    public byte getMinNote() {
        return notes.length == 0 ? -1 : minNote;
    }

    private boolean isAvailableNote(byte note) {
        return !ArrayUtils.contains(notes, note) && note >= 0 && note <= 24;
    }

    public boolean addOneNote(byte note) {
        if (isAvailableNote(note)) {
            setNotes(ArrayUtils.add(notes, note));
            return true;
        }
        return false;
    }

    public void addNotes(byte... newNotes) {
        byte len = 0;
        byte[] availableNewNotes = new byte[newNotes.length];
        for (byte note : newNotes) {
            if (isAvailableNote(note)) {
                availableNewNotes[len++] = note;
            }
        }
        if (len > 0) {
            byte[] noteArray = new byte[notes.length + len];
            System.arraycopy(notes, 0, noteArray, 0, notes.length);
            System.arraycopy(availableNewNotes, 0, noteArray, notes.length, len);
            setNotes(noteArray);
        }
    }
}
