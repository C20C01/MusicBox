package io.github.c20c01.cc_mb.data;

import io.github.c20c01.cc_mb.util.TagData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

// 重写，尚未完全实现

public class NoteGridData$ extends SavedData implements TagData<ListTag> {
    public static final String DATA_KEY = "Notes";
    public static final byte MAX_PAGES = 64;
    private ArrayList<Page> pages = new ArrayList<>(List.of(new Page()));

    public static NoteGridData$ ofPages(Page... pages) {
        return new NoteGridData$().setPages(pages);
    }

    public static NoteGridData$ ofBook(ItemStack book) {
        return new NoteGridData$().loadBook(book);
    }

    public static NoteGridData$ ofTag(ListTag noteGridTag) {
        return new NoteGridData$().loadTag(noteGridTag);
    }

    /**
     * Get the data of the note grid with the given id from the over world's data storage.
     */
    @Nullable
    public static NoteGridData$ ofId(MinecraftServer server, int noteGridId) {
        String key = makeKey(noteGridId);
        return server.overworld().getDataStorage().get(NoteGridData$::load, key);
    }

    public static NoteGridData$ load(CompoundTag tag) {
        ListTag listTag = tag.getList(DATA_KEY, Tag.TAG_LIST);
        return NoteGridData$.ofTag(listTag);
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

    @Override
    public NoteGridData$ loadTag(ListTag noteGridTag) {
        ArrayList<Page> pages = new ArrayList<>(noteGridTag.size());
        for (Tag pageTag : noteGridTag) {
            pages.add(Page.ofTag((ListTag) pageTag));
        }
        return setPages(pages);
    }

    @Override
    public ListTag toTag() {
        ListTag noteGridTag = new ListTag();
        for (byte page = 0; page < pages.size(); page++) {
            noteGridTag.add(getPage(page).toTag());
        }
        return noteGridTag;
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
}
