package io.github.c20c01.cc_mb.util;

import io.github.c20c01.cc_mb.CCMain;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NoteBlock;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
                beats1[i] = new Beat(new byte[]{note});
            }
        }

        Beat[] beats2 = new Beat[Page.SIZE];
        for (int i = 0; i < beats2.length; i++) {
            beats2[i] = new Beat(new byte[]{(byte) (i % 25)});
        }

        NoteGridData.saveToTag(noteGrid, new Page[]{new Page(beats1), new Page(beats2)});
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
                pages.add(Page.of((ListTag) pagesTag.get(i)));
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
        for (int i = 0; i < Math.min(pages.length, otherPages.length); i++) {
            Beat[] beats = pages[i].beats;
            Beat[] otherBeats = otherPages[i].beats;
            for (int j = 0; j < Page.SIZE; j++) {
                beats[j].addNotes(otherBeats[j].getNotes());
            }
        }
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
            pages[i] = Page.of(codeOfPages.getString(i));
        }

        return pages;
    }

    /**
     * @param key Note Block Studio 中的键位
     * @return 音符盒的 0~24 音符, -1 表示无效的键位
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

    public static class Page {
        public static final byte SIZE = 64;
        private final Beat[] beats;

        public Page() {
            this.beats = new Beat[SIZE];
            for (byte beat = 0; beat < SIZE; beat++) {
                beats[beat] = Beat.empty();
            }
        }

        public Page(Beat[] beats) {
            this.beats = new Beat[SIZE];
            System.arraycopy(beats, 0, this.beats, 0, Math.min(beats.length, SIZE));
            for (byte beat = 0; beat < SIZE; beat++) {
                if (this.beats[beat] == null) {
                    this.beats[beat] = Beat.empty();
                }
            }
        }

        @Override
        public String toString() {
            return "Page:" + Arrays.toString(beats);
        }

        public ListTag toTag() {
            ListTag pageTag = new ListTag();
            for (byte beat = 0; beat < SIZE; beat++) {
                pageTag.add(getBeat(beat).toTag());
            }
            return pageTag;
        }

        public static Page of(ListTag pageTag) {
            Page page = new Page();
            for (byte beat = 0; beat < SIZE; beat++) {
                page.beats[beat] = Beat.of(pageTag.get(beat));
            }
            return page;
        }

        public static Page of(String codeOfPage) {
            Page page = new Page();
            String[] codesOfBeat = codeOfPage.split("\\.");
            for (int beat = 0; beat < Math.min(SIZE, codesOfBeat.length); beat++) {
                page.beats[beat] = Beat.of(codesOfBeat[beat]);
            }
            return page;
        }

        public Beat getBeat(byte index) throws ArrayIndexOutOfBoundsException {
            return beats[index];
        }
    }

    public static class Beat {
        private byte[] notes;

        public Beat(byte[] notes) {
            Arrays.sort(notes);
            this.notes = notes;
        }

        public static Beat empty() {
            return new Beat(new byte[0]);
        }

        @Override
        public String toString() {
            return "Beat:" + Arrays.toString(notes);
        }

        private boolean isAvailableNote(byte note) {
            return !ArrayUtils.contains(notes, note) && note >= 0 && note <= 24;
        }

        public boolean addOneNote(byte note) {
            if (isAvailableNote(note)) {
                notes = ArrayUtils.add(notes, note);
                Arrays.sort(notes);
                return true;
            }
            return false;
        }

        public void addNotes(byte[] newNotes) {
            ArrayList<Byte> availableNotes = new ArrayList<>();
            for (byte note : newNotes) {
                if (isAvailableNote(note)) {
                    availableNotes.add(note);
                }
            }
            notes = ArrayUtils.addAll(this.notes, ArrayUtils.toPrimitive(availableNotes.toArray(new Byte[0])));
            Arrays.sort(notes);
        }

        public ByteArrayTag toTag() {
            return new ByteArrayTag(notes);
        }

        public static Beat of(Tag beatTag) {
            return new Beat(((ByteArrayTag) beatTag).getAsByteArray());
        }

        public static Beat of(String codeOfBeat) {
            if (codeOfBeat.isEmpty()) {
                return Beat.empty();
            }

            Set<Byte> notes = new HashSet<>();
            for (char c : codeOfBeat.toCharArray()) {
                byte note = getNoteFromKey(c);
                if (note != -1) {
                    notes.add(note);
                }
            }
            return new Beat(ArrayUtils.toPrimitive(notes.toArray(new Byte[0])));
        }

        public byte[] getNotes() {
            return notes;
        }

        public byte getMinNote() {
            return notes[0];
        }

        public boolean notEmpty() {
            return notes.length > 0;
        }

        public byte play(Level level, BlockPos blockPos, Holder<SoundEvent> holder, long soundSeed) {
            double x = (double) blockPos.getX() + 0.5D;
            double y = (double) blockPos.getY() + 0.5D;
            double z = (double) blockPos.getZ() + 0.5D;

            for (byte note : notes) {
                float pitch = NoteBlock.getPitchFromNote(note);
                level.playSeededSound(null, x, y, z, holder, SoundSource.RECORDS, 3.0F, pitch, soundSeed);
            }

            return getMinNote();
        }
    }
}
