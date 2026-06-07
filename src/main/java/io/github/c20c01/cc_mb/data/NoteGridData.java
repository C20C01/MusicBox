package io.github.c20c01.cc_mb.data;

import io.github.c20c01.cc_mb.MusicBox;
import io.github.c20c01.cc_mb.util.NoteGridUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WritableBookContent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public final class NoteGridData {
    public static final byte MAX_SIZE = 64;
    private final ArrayList<Page> pages;
    private boolean dirty = true;
    private int hashCode = 0;

    private NoteGridData() {
        this.pages = new ArrayList<>(List.of(new Page()));
    }

    public NoteGridData(Collection<Page> pages) {
        this.pages = new ArrayList<>(pages);
        this.pages.replaceAll(page -> page == null ? new Page() : page);
    }

    public static NoteGridData empty() {
        return new NoteGridData();
    }

    public static NoteGridData ofBytes(byte[] bytes) {
        return new NoteGridCode(bytes).toData();
    }

    public static NoteGridData ofPages(Page... pages) {
        return new NoteGridData(new ArrayList<>(Arrays.asList(pages)));
    }

    public static NoteGridData ofBook(ItemStack book) {
        Stream<String> codes = book.getOrDefault(DataComponents.WRITABLE_BOOK_CONTENT, WritableBookContent.EMPTY).getPages(true);
        ArrayList<Page> pages = new ArrayList<>();
        codes.limit(MAX_SIZE).forEach(code -> pages.add(Page.ofCode(code)));
        return new NoteGridData(pages);
    }

    public static NoteGridData ofNoteGrid(ItemStack noteGrid) {
        return NoteGridCode.of(noteGrid).toData();
    }

    public NoteGridData deepCopy() {
        return NoteGridUtils.join(NoteGridData.ofPages(new Page[this.size()]), this);
    }

    public byte[] toBytes() {
        return NoteGridCode.of(this).code();
    }

    public void saveToNoteGrid(ItemStack noteGrid) {
        noteGrid.set(MusicBox.NOTE_GRID_DATA.get(), NoteGridCode.of(this));
    }

    public void markDirty() {
        this.dirty = true;
    }

    @Override
    public String toString() {
        return "NoteGrid:" + pages;
    }

    @Override
    public int hashCode() {
        if (dirty) {
            hashCode = pages.hashCode();
            dirty = false;
        }
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof NoteGridData noteGridData) {
            return pages.equals(noteGridData.pages);
        }
        return super.equals(obj);
    }

    public Page getPage(byte index) {
        return pages.get(index);
    }

    public ArrayList<Page> getPages() {
        return pages;
    }

    public byte size() {
        return (byte) pages.size();
    }
}
