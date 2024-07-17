package io.github.c20c01.cc_mb.data;

import io.github.c20c01.cc_mb.CCMain;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;

public class NoteGridData {
    private static final byte MAX_PAGE = 64;

    public static ItemStack getTestingGrid() {
        ItemStack noteGrid = new ItemStack(CCMain.NOTE_GRID_ITEM.get());

        byte[] notes = new byte[]{
                6, 6, 13, 13, 15, 15, 13, -1,
                11, 11, 10, 10, 8, 8, 6, -1,
                13, 13, 11, 11, 10, 10, 8, -1,
                13, 13, 11, 11, 10, 10, 8, -1,
                6, 6, 13, 13, 15, 15, 13, -1,
                11, 11, 10, 10, 8, 8, 6
        };
        Beat[] beats1 = new Beat[notes.length];
        for (int i = 0; i < beats1.length; i++) {
            byte note = notes[i];
            if (note != -1) {
                beats1[i] = Beat.ofNotes(new byte[]{note});
            }
        }

        Beat[] beats2 = new Beat[Page.BEATS_SIZE];
        for (int i = 0; i < beats2.length; i++) {
            beats2[i] = Beat.ofNotes(new byte[]{(byte) (i % 25)});
        }

        NoteGridData$ data = NoteGridData$.ofPages(Page.ofBeats(beats1), Page.ofBeats(beats2));


        NoteGridData.saveToTag(noteGrid, new Page[]{Page.ofBeats(beats1), Page.ofBeats(beats2)});
        noteGrid.setHoverName(Component.literal("Testing Note Grid").withStyle(ChatFormatting.GOLD));
        return noteGrid;
    }

    public static void saveToTag(ItemStack noteGrid, Page[] pages) {
        if (pages.length == 0) return;
        ListTag pagesTag = new ListTag();
        for (int i = 0; i < Math.min(pages.length, MAX_PAGE); i++) {
            pagesTag.add(pages[i].toTag());
        }
        noteGrid.getOrCreateTag().put("Notes", pagesTag);
    }

    public static Page[] readFromTag(ItemStack noteGrid) {
        CompoundTag tag = noteGrid.getOrCreateTag();
        if (tag.contains("Notes")) {
            ArrayList<Page> pages = new ArrayList<>();
            ListTag pagesTag = tag.getList("Notes", Tag.TAG_LIST);
            for (int i = 0; i < Math.min(pagesTag.size(), MAX_PAGE); i++) {
                pages.add(Page.ofTag((ListTag) pagesTag.get(i)));
            }
            return pages.toArray(new Page[0]);
        }

        return new Page[]{new Page()};
    }

    /**
     * 将两张纸带连接起来，并截取最大页数内的部分
     *
     * @param noteGrid  前面的纸带
     * @param otherGrid 后面的纸带
     * @return 连接后的纸带
     */
    public static Page[] connectGrid(ItemStack noteGrid, ItemStack otherGrid) {
        Page[] pages = readFromTag(noteGrid);
        Page[] otherPages = readFromTag(otherGrid);
        int size = MAX_PAGE - pages.length;
        if (size < otherPages.length) {
            otherPages = Arrays.copyOf(otherPages, size);
        }
        return ArrayUtils.addAll(pages, otherPages);
    }

    /**
     * 将后面的纸带的孔洞叠加到前面的纸带上
     *
     * @param noteGrid  前面的纸带
     * @param otherGrid 后面的纸带
     * @return 叠加后的纸带
     */
    public static Page[] superposeGrid(ItemStack noteGrid, ItemStack otherGrid) {
        return superpose(readFromTag(noteGrid), readFromTag(otherGrid));
    }

    private static Page[] superpose(Page[] pages, Page[] otherPages) {
//        for (int i = 0; i < Math.min(pages.length, otherPages.length); i++) {
//            Beat[] beats = pages[i].getBeats();
//            Beat[] otherBeats = otherPages[i].getBeats();
//            for (int j = 0; j < Page.BEATS_SIZE; j++) {
//                beats[j].addNotes(otherBeats[j].getNotes());
//            }
//        }
        return pages;
    }

    public static Page[] superposeGridByBook(ItemStack noteGrid, ItemStack book) {
        Page[] pages = readFromTag(noteGrid);
        Page[] otherPages = readFromBook(book);
        if (otherPages == null) {
            return pages;
        }
        return superpose(pages, otherPages);
    }

    @Nullable
    private static Page[] readFromBook(ItemStack book) {
        CompoundTag tag = book.getTag();
        if (tag == null) {
            return null;
        }

        ListTag codeOfPages = tag.getList("pages", Tag.TAG_STRING);
        int size = Math.min(codeOfPages.size(), MAX_PAGE);
        Page[] pages = new Page[size];
        for (int i = 0; i < size; i++) {
            pages[i] = Page.ofCode(codeOfPages.getString(i));
        }

        return pages;
    }
}
