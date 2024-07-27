package io.github.c20c01.cc_mb.data;

import io.github.c20c01.cc_mb.CCMain;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class NoteGridData extends SavedData {
    public static final String DATA_KEY = "Notes";
    public static final byte MAX_SIZE = 64;
    private ArrayList<Page> pages = new ArrayList<>(List.of(new Page()));

    public static NoteGridData ofPages(Page... pages) {
        return new NoteGridData().loadPages(pages);
    }

    public static NoteGridData ofBytes(byte[] data) {
        return new NoteGridData().loadPages(new Decoder().decode(data));
    }

    public static NoteGridData ofBook(ItemStack book) {
        return new NoteGridData().loadBook(book);
    }

    public static NoteGridData ofNoteGrid(ItemStack noteGrid) {
        return new NoteGridData().loadNoteGrid(noteGrid);
    }

    private NoteGridData loadNoteGrid(ItemStack noteGrid) {
        CompoundTag tag = noteGrid.getTag();
        if (tag == null || !tag.contains(DATA_KEY)) {
            return this;
        }
        return loadTag((ByteArrayTag) tag.get(DATA_KEY));
    }

    public NoteGridData loadBook(ItemStack book) {
        CompoundTag tag = book.getTag();
        if (tag == null) {
            return this;
        }
        ListTag codeOfPages = tag.getList("pages", Tag.TAG_STRING);
        if (codeOfPages.isEmpty()) {
            return this;
        }
        int size = Math.min(codeOfPages.size(), MAX_SIZE);
        Page[] pages = new Page[size];
        for (int i = 0; i < size; i++) {
            pages[i] = Page.ofCode(codeOfPages.getString(i));
        }
        return loadPages(pages);
    }

    public NoteGridData loadTag(@Nullable ByteArrayTag noteGridTag) {
        if (noteGridTag == null) {
            return this;
        }
        byte[] data = noteGridTag.getAsByteArray();
        return loadPages(new Decoder().decode(data));
    }

    public NoteGridData deepCopy() {
        NoteGridData temp = NoteGridData.ofPages(new Page[size()]);
        for (byte page = 0; page < size(); page++) {
            for (byte beat = 0; beat < Page.BEATS_SIZE; beat++) {
                temp.getPage(page).getBeat(beat).addNotes(getPage(page).getBeat(beat).getNotes());
            }
        }
        return temp;
    }

    public ByteArrayTag toTag() {
        return new ByteArrayTag(new Encoder().encode(pages));
    }

    public ItemStack toNoteGrid() {
        return saveToNoteGrid(new ItemStack(CCMain.NOTE_GRID_ITEM.get()));
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.put(DATA_KEY, toTag());
        return tag;
    }

    public ItemStack saveToNoteGrid(ItemStack noteGrid) {
        if (noteGrid.is(CCMain.NOTE_GRID_ITEM.get())) {
            CompoundTag tag = noteGrid.getOrCreateTag();
            tag.put(DATA_KEY, toTag());
        }
        return noteGrid;
    }

    @Override
    public String toString() {
        return "NoteGrid:" + pages;
    }

    public Page getPage(byte index) {
        return pages.get(index);
    }

    public byte size() {
        return (byte) pages.size();
    }

    public ArrayList<Page> getPages() {
        return pages;
    }

    private NoteGridData loadPages(Page[] pages) {
        return loadPages(Arrays.asList(pages));
    }

    private NoteGridData loadPages(Collection<Page> pages) {
        this.pages = new ArrayList<>(pages);
        this.pages.replaceAll(page -> page == null ? new Page() : page);
        return this;
    }

    private static class Decoder {
        final ArrayList<Page> PAGES = new ArrayList<>();
        final ArrayList<Beat> BEATS = new ArrayList<>(Page.BEATS_SIZE);
        final ArrayList<Byte> NOTES = new ArrayList<>(5);

        ArrayList<Page> decode(byte[] data) {
            for (byte b : data) {
                if (b > 0) {
                    handleNote(b);
                } else {
                    handleFlag(b);
                }
            }
            return PAGES;
        }

        void handleFlag(byte b) {
            finishBeat();
            if (b == 0) {
                finishPage();
            } else {
                byte emptyBeats = (byte) (-b - 1);
                for (byte i = 0; i < emptyBeats; i++) {
                    BEATS.add(null);
                }
            }
        }

        void handleNote(byte b) {
            byte note = (byte) (b - 1);
            if (Beat.isAvailableNote(note)) {
                NOTES.add(note);
            }
        }

        void finishBeat() {
            if (!NOTES.isEmpty()) {
                BEATS.add(Beat.ofNotes(NOTES));
                NOTES.clear();
            }
        }

        void finishPage() {
            PAGES.add(Page.ofBeats(BEATS));
            BEATS.clear();
        }
    }

    private static class Encoder {
        final ArrayList<Byte> DATA = new ArrayList<>(1024);
        byte emptyBeats = 0;

        ArrayList<Byte> encode(ArrayList<Page> pages) {
            for (Page page : pages) {
                for (byte i = 0; i < Page.BEATS_SIZE; i++) {
                    if (page.isEmptyBeat(i)) {
                        emptyBeats++;
                    } else {
                        addFlag();
                        addBeat(page.getBeat(i));
                    }
                }
                finishPage();
            }
            return DATA;
        }

        void addBeat(Beat beat) {
            byte[] notes = beat.getNotes();
            DATA.ensureCapacity(DATA.size() + notes.length);
            for (byte note : notes) {
                DATA.add((byte) (note + 1));
            }
        }

        void addFlag() {
            byte flag = (byte) (-emptyBeats - 1);
            DATA.add(flag);
            emptyBeats = 0;
        }

        void finishPage() {
            DATA.add((byte) 0);
            emptyBeats = 0;
        }
    }
}
