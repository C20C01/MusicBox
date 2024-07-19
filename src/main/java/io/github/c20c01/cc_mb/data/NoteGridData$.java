package io.github.c20c01.cc_mb.data;

import io.github.c20c01.cc_mb.util.TagData;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

// 重写，尚未完全实现

public class NoteGridData$ extends SavedData implements TagData<ByteArrayTag> {
    public static final String DATA_KEY = "Notes";
    public static final byte MAX_PAGES = 64;
    private ArrayList<Page> pages = new ArrayList<>(List.of(new Page()));

    public static NoteGridData$ ofPages(Page... pages) {
        return new NoteGridData$().setPages(pages);
    }

    public static NoteGridData$ ofPages(Collection<Page> pages) {
        return new NoteGridData$().setPages(pages);
    }

    public static NoteGridData$ ofBook(ItemStack book) {
        return new NoteGridData$().loadBook(book);
    }

    public static NoteGridData$ ofTag(CompoundTag noteGridTag) {
        return new NoteGridData$().loadTag((ByteArrayTag) noteGridTag.get(DATA_KEY));
    }

    /**
     * Get the data of the note grid with the given id from the over world's data storage.
     */
    @Nullable
    public static NoteGridData$ ofId(MinecraftServer server, int noteGridId) {
        String key = makeKey(noteGridId);
        return server.overworld().getDataStorage().get(NoteGridData$::ofTag, key);
    }

    @Nullable
    @OnlyIn(Dist.CLIENT)
    public static NoteGridData$ ofId(int noteGridId, Consumer<NoteGridData$> updater) {
        return ClientNoteGridManager.getNoteGridData(noteGridId, updater);
    }

    public static String makeKey(Integer noteGridId) {
        return "NoteGrid_" + noteGridId;
    }

    public NoteGridData$ loadBook(ItemStack book) {
        CompoundTag tag = book.getTag();
        if (tag == null) {
            return this;
        }
        ListTag codeOfPages = tag.getList("pages", Tag.TAG_STRING);
        if (codeOfPages.isEmpty()) {
            return this;
        }
        int size = Math.min(codeOfPages.size(), MAX_PAGES);
        Page[] pages = new Page[size];
        for (int i = 0; i < size; i++) {
            pages[i] = Page.ofCode(codeOfPages.getString(i));
        }
        return setPages(pages);
    }

    public NoteGridData$ loadTag(@Nullable ByteArrayTag noteGridTag) {
        if (noteGridTag == null) {
            return this;
        }
        byte[] data = noteGridTag.getAsByteArray();
        ArrayList<Page> pages = new Decoder().decode(data);
        return setPages(pages);
    }

    public ByteArrayTag toTag() {
        ArrayList<Byte> data = new Encoder().encode(pages);
        return new ByteArrayTag(data);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.put(DATA_KEY, toTag());
        return tag;
    }

    /**
     * Save this data to the over world's data storage.
     */
    public void save(MinecraftServer server, int noteGridId) {
        String key = makeKey(noteGridId);
        server.overworld().getDataStorage().set(key, this);
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

    public NoteGridData$ setPages(Page[] pages) {
        return setPages(Arrays.asList(pages));
    }

    public NoteGridData$ setPages(Collection<Page> pages) {
        this.pages = new ArrayList<>(pages);
        this.pages.replaceAll(page -> page == null ? new Page() : page);
        setDirty();
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
