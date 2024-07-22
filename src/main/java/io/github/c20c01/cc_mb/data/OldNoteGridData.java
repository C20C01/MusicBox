package io.github.c20c01.cc_mb.data;

import net.minecraft.world.item.ItemStack;

public class OldNoteGridData {
    public static void saveToTag(ItemStack noteGrid, Page[] pages) {
//        if (pages.length == 0) return;
//        ListTag pagesTag = new ListTag();
//        for (int i = 0; i < Math.min(pages.length, MAX_PAGE); i++) {
//            pagesTag.add(pages[i].toTag());
//        }
//        noteGrid.getOrCreateTag().put("Notes", pagesTag);
    }

    public static Page[] readFromTag(ItemStack noteGrid) {
//        CompoundTag tag = noteGrid.getOrCreateTag();
//        if (tag.contains("Notes")) {
//            ArrayList<Page> pages = new ArrayList<>();
//            ListTag pagesTag = tag.getList("Notes", Tag.TAG_LIST);
//            for (int i = 0; i < Math.min(pagesTag.size(), MAX_PAGE); i++) {
//                pages.add(Page.ofTag((ListTag) pagesTag.get(i)));
//            }
//            return pages.toArray(new Page[0]);
//        }

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
//        Page[] pages = readFromTag(noteGrid);
//        Page[] otherPages = readFromTag(otherGrid);
//        int size = MAX_PAGE - pages.length;
//        if (size < otherPages.length) {
//            otherPages = Arrays.copyOf(otherPages, size);
//        }
//        return ArrayUtils.addAll(pages, otherPages);
        return new Page[0];
    }

    /**
     * 将后面的纸带的孔洞叠加到前面的纸带上
     *
     * @param noteGrid  前面的纸带
     * @param otherGrid 后面的纸带
     * @return 叠加后的纸带
     */
    public static Page[] superposeGrid(ItemStack noteGrid, ItemStack otherGrid) {
//        return superpose(readFromTag(noteGrid), readFromTag(otherGrid));
        return new Page[0];
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
//        Page[] pages = readFromTag(noteGrid);
//        Page[] otherPages = readFromBook(book);
//        if (otherPages == null) {
//            return pages;
//        }
//        return superpose(pages, otherPages);
        return new Page[0];
    }
}
