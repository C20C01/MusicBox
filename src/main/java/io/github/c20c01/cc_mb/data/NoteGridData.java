package io.github.c20c01.cc_mb.data;

import com.mojang.logging.LogUtils;
import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.item.NoteGrid;
import io.github.c20c01.cc_mb.util.CollectionUtils;
import io.github.c20c01.cc_mb.util.TagData;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

// TODO 纸带的连接
// TODO 纸带自其他纸带、书本的复制
// TODO 纸带的裁剪

public class NoteGridData extends SavedData implements TagData<ByteArrayTag> {
    public static final String DATA_KEY = "Notes";
    public static final byte MAX_PAGES = 64;
    private static final ArrayList<Supplier<PredefinedSong>> PREDEFINED_SONGS = new ArrayList<>();

    static {
        addPredefinedSong("Little Star", new byte[]{
                -1, 7, -1, 7, -1, 14, -1, 14, -1, 16, -1, 16, -1, 14,
                -2, 12, -1, 12, -1, 11, -1, 11, -1, 9, -1, 9, -1, 7,
                -2, 14, -1, 14, -1, 12, -1, 12, -1, 11, -1, 11, -1, 9,
                -2, 14, -1, 14, -1, 12, -1, 12, -1, 11, -1, 11, -1, 9,
                -2, 7, -1, 7, -1, 14, -1, 14, -1, 16, -1, 16, -1, 14,
                -2, 12, -1, 12, -1, 11, -1, 11, -1, 9, -1, 9, -1, 7, 0
        });
    }

    private ArrayList<Page> pages = new ArrayList<>(List.of(new Page()));

    private NoteGridData() {

    }

    public NoteGridData(NoteGridData data) {
        this.pages = new ArrayList<>(data.pages);
    }

    public static ArrayList<ItemStack> getPredefinedSongs() {
        ArrayList<ItemStack> items = new ArrayList<>();
        for (Supplier<PredefinedSong> song : PREDEFINED_SONGS) {
            items.add(song.get().toItemStack());
        }
        return items;
    }

    public static void addPredefinedSong(String name, byte[] data) {
        PREDEFINED_SONGS.add(() -> new PredefinedSong(PREDEFINED_SONGS.size(), name, NoteGridData.ofBytes(data)));
    }

    /**
     * Get the predefined note grid data by its id.
     *
     * @param noteGridId 1~64 for empty pages, 65~ for predefined songs.<p>
     *                   {@code ofPredefinedId(5)} returns an empty note grid with 5 page.<p>
     *                   {@code ofPredefinedId(65)} returns the PREDEFINED_SONGS.get(0).
     */
    protected static NoteGridData ofPredefinedId(int noteGridId) {
        if (noteGridId <= MAX_PAGES) {
            return ofPages(new Page[noteGridId]);
        } else {
            int index = noteGridId - MAX_PAGES - 1;
            if (index >= PREDEFINED_SONGS.size()) {
                LogUtils.getLogger().warn("Predefined song with id {} does not exist", noteGridId);
                return ofPages(new Page[1]);
            } else {
                return PREDEFINED_SONGS.get(index).get().data;
            }
        }
    }

    public static NoteGridData ofPages(Page... pages) {
        return new NoteGridData().loadPages(pages);
    }

    public static NoteGridData ofPages(Collection<Page> pages) {
        return new NoteGridData().loadPages(pages);
    }

    public static NoteGridData ofBytes(byte[] data) {
        return new NoteGridData().loadPages(new Decoder().decode(data));
    }

    public static NoteGridData ofBook(ItemStack book) {
        return new NoteGridData().loadBook(book);
    }

    /**
     * Get the note grid data by its id from ServerNoteGridManager.
     *
     * @param noteGridId 1~ for custom songs data, -64~-1 for empty pages, ~-65 for predefined songs.
     */
    @Nullable
    public static NoteGridData ofId(MinecraftServer server, int noteGridId) {
        if (noteGridId < 0) {
            return ofPredefinedId(-noteGridId);
        } else {
            return ServerNoteGridManager.getNoteGridData(server, noteGridId);
        }
    }

    /**
     * Get the note grid data by its id from ClientNoteGridManager.
     *
     * @param noteGridId 1~ for custom songs data, -64~-1 for empty pages, ~-65 for predefined songs.
     */
    @Nullable
    public static NoteGridData ofId(int noteGridId, @Nullable Consumer<NoteGridData> updater) {
        if (noteGridId < 0) {
            return ofPredefinedId(-noteGridId);
        } else {
            return ClientNoteGridManager.getNoteGridData(noteGridId, updater);
        }
    }

    public static NoteGridData ofDataStorageTag(CompoundTag noteGridDataTag) {
        return new NoteGridData().loadTag((ByteArrayTag) noteGridDataTag.get(DATA_KEY));
    }

    public static String makeKey(Integer noteGridId) {
        return "NoteGrid_" + noteGridId;
    }

    /**
     * @param index The index of the predefined song.
     * @return The id of the predefined song.
     */
    public static int getPredefinedId(int index) {
        return -(MAX_PAGES + index);
    }

    /**
     * Connect two note grid data.
     * The first note grid data will contain all pages of the second note grid data.
     * The second note grid data will be empty.
     * <p>
     * Connected data can not bigger than {@link NoteGridData#MAX_PAGES}.
     *
     * @param a The first note grid data
     * @param b The second note grid data
     */
    public static void connect(NoteGridData a, NoteGridData b) {
        if (a.getPages().size() + b.getPages().size() > NoteGridData.MAX_PAGES) {
            throw new IllegalArgumentException("The connected data can not bigger than " + NoteGridData.MAX_PAGES);
        }
        a.getPages().addAll(b.getPages());
        b.getPages().clear();
        a.setDirty();
        b.setDirty();
    }

    /**
     * Cut the note grid data at the end of specified page into two note grid data.
     * <p>
     * Example: cutAt(data, 3) will cut the data into two parts,
     * the first part contains page [0,2], the second part contains page [3, data.size()].
     */
    public static int cutAt(MinecraftServer server, NoteGridData data, int pageIndex) {
        if (pageIndex < 0 || pageIndex >= data.getPages().size()) {
            throw new IllegalArgumentException("The page index is out of range.");
        }
        NoteGridData secondPart = new NoteGridData();
        secondPart.getPages().addAll(data.getPages().subList(pageIndex, data.getPages().size()));
        data.getPages().removeAll(secondPart.getPages());
        data.setDirty();
        return ServerNoteGridManager.saveNewData(server, secondPart);
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
        int size = Math.min(codeOfPages.size(), MAX_PAGES);
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

    public ByteArrayTag toTag() {
        return new ByteArrayTag(new Encoder().encode(pages));
    }

    public byte[] toBytes() {
        return CollectionUtils.toArray(new Encoder().encode(pages));
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
        if (noteGridId < 0) {
            throw new IllegalArgumentException("Id must be positive");
        }
        String key = makeKey(noteGridId);
        ServerNoteGridManager.makeDirty(noteGridId);
        server.overworld().getDataStorage().set(key, this);
        setDirty();
    }

    @Override
    public String toString() {
        return "NoteGrid:" + pages;
    }

    /**
     * Call {@link #save(MinecraftServer, int)} or {@link #setDirty()} after modifying the data.
     */
    public Page getPage(byte index) {
        return pages.get(index);
    }

    public byte size() {
        return (byte) pages.size();
    }

    protected ArrayList<Page> getPages() {
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

    private record PredefinedSong(int index, String name, NoteGridData data) {
        public ItemStack toItemStack() {
            ItemStack itemStack = new ItemStack(CCMain.NOTE_GRID_ITEM.get());
            NoteGrid.setId(itemStack, getPredefinedId(index));
            itemStack.setHoverName(Component.literal(name));
            return itemStack;
        }
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
