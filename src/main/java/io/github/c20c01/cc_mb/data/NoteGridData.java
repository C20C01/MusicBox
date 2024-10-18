package io.github.c20c01.cc_mb.data;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.util.NoteGridUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WritableBookContent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class NoteGridData {
    public static final byte MAX_SIZE = 64;
    private final ArrayList<Page> PAGES;

    private NoteGridData() {
        this.PAGES = new ArrayList<>(List.of(new Page()));
    }

    protected NoteGridData(ArrayList<Page> pages) {
        this.PAGES = pages;
        this.PAGES.replaceAll(page -> page == null ? new Page() : page);
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
        noteGrid.set(CCMain.NOTE_GRID_DATA.get(), NoteGridCode.of(this));
    }

    @Override
    public String toString() {
        return "NoteGrid:" + PAGES;
    }

    @Override
    public int hashCode() {
        return PAGES.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof NoteGridData noteGridData) {
            return PAGES.equals(noteGridData.PAGES);
        }
        return super.equals(obj);
    }

    public Page getPage(byte index) {
        return PAGES.get(index);
    }

    public ArrayList<Page> getPages() {
        return PAGES;
    }

    public byte size() {
        return (byte) PAGES.size();
    }
}
