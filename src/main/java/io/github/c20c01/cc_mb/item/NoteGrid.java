package io.github.c20c01.cc_mb.item;

import io.github.c20c01.cc_mb.block.MusicBoxBlock;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault

public class NoteGrid extends Item {
    private static final byte MAX_PAGE = 64;

    public NoteGrid() {
        super(new Properties().stacksTo(1));
    }

    public static ItemStack changeToTestingGrid(ItemStack noteGrid) {
        saveToTag(noteGrid, new Page[]{new Page(new Beat[]{new Beat(new byte[]{6}), new Beat(new byte[]{6}), new Beat(new byte[]{13}), new Beat(new byte[]{13}), new Beat(new byte[]{15}), new Beat(new byte[]{15}), new Beat(new byte[]{13}), Beat.empty(), new Beat(new byte[]{11}), new Beat(new byte[]{11}), new Beat(new byte[]{10}), new Beat(new byte[]{10}), new Beat(new byte[]{8}), new Beat(new byte[]{8}), new Beat(new byte[]{6}), Beat.empty(), new Beat(new byte[]{13}), new Beat(new byte[]{13}), new Beat(new byte[]{11}), new Beat(new byte[]{11}), new Beat(new byte[]{10}), new Beat(new byte[]{10}), new Beat(new byte[]{8}), Beat.empty(), new Beat(new byte[]{13}), new Beat(new byte[]{13}), new Beat(new byte[]{11}), new Beat(new byte[]{11}), new Beat(new byte[]{10}), new Beat(new byte[]{10}), new Beat(new byte[]{8}), Beat.empty(), new Beat(new byte[]{6}), new Beat(new byte[]{6}), new Beat(new byte[]{13}), new Beat(new byte[]{13}), new Beat(new byte[]{15}), new Beat(new byte[]{15}), new Beat(new byte[]{13}), Beat.empty(), new Beat(new byte[]{11}), new Beat(new byte[]{11}), new Beat(new byte[]{10}), new Beat(new byte[]{10}), new Beat(new byte[]{8}), new Beat(new byte[]{8}), new Beat(new byte[]{6}), Beat.empty()}), new Page(new Beat[]{new Beat(new byte[]{0, 24}), new Beat(new byte[]{1}), new Beat(new byte[]{2}), new Beat(new byte[]{3}), new Beat(new byte[]{4}), new Beat(new byte[]{5}), new Beat(new byte[]{6}), new Beat(new byte[]{7}), new Beat(new byte[]{8}), new Beat(new byte[]{9}), new Beat(new byte[]{10}), new Beat(new byte[]{11}), new Beat(new byte[]{12}), new Beat(new byte[]{13}), new Beat(new byte[]{14}), new Beat(new byte[]{15}), new Beat(new byte[]{16}), new Beat(new byte[]{17}), new Beat(new byte[]{18}), new Beat(new byte[]{19}), new Beat(new byte[]{20}), new Beat(new byte[]{21}), new Beat(new byte[]{22}), new Beat(new byte[]{0, 23}), new Beat(new byte[]{0, 23}), new Beat(new byte[]{1}), new Beat(new byte[]{2}), new Beat(new byte[]{3}), new Beat(new byte[]{4}), new Beat(new byte[]{5}), new Beat(new byte[]{6}), new Beat(new byte[]{7}), new Beat(new byte[]{8}), new Beat(new byte[]{9}), new Beat(new byte[]{10}), new Beat(new byte[]{11}), new Beat(new byte[]{12}), new Beat(new byte[]{13}), new Beat(new byte[]{14}), new Beat(new byte[]{15}), new Beat(new byte[]{16}), new Beat(new byte[]{17}), new Beat(new byte[]{18}), new Beat(new byte[]{19}), new Beat(new byte[]{20}), new Beat(new byte[]{21}), new Beat(new byte[]{22}), new Beat(new byte[]{0, 23}), new Beat(new byte[]{0, 23}), new Beat(new byte[]{1}), new Beat(new byte[]{2}), new Beat(new byte[]{3}), new Beat(new byte[]{4}), new Beat(new byte[]{5}), new Beat(new byte[]{6}), new Beat(new byte[]{7}), new Beat(new byte[]{8}), new Beat(new byte[]{9}), new Beat(new byte[]{10}), new Beat(new byte[]{11}), new Beat(new byte[]{12}), new Beat(new byte[]{13}), new Beat(new byte[]{14}), new Beat(new byte[]{0, 15, 23})})});
        noteGrid.setHoverName(Component.literal("Testing Note Grid"));
        return noteGrid;
    }

    public record Tooltip(NoteGrid.Page page, Byte numberOfPages) implements TooltipComponent {
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack itemStack) {
        Page[] pages = readFromTag(itemStack);
        return Optional.of(new Tooltip(pages[0], (byte) pages.length));
    }

    @Override
    public void appendHoverText(ItemStack p_41421_, @org.jetbrains.annotations.Nullable Level p_41422_, List<Component> p_41423_, TooltipFlag p_41424_) {
        super.appendHoverText(p_41421_, p_41422_, p_41423_, p_41424_);
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

    public static class Page {
        public static final byte SIZE = 64;
        private final Beat[] beats;

        public Page() {
            this.beats = new Beat[SIZE];
            for (byte beat = 0; beat < SIZE; beat++) {
                this.beats[beat] = Beat.empty();
            }
        }

        public Page(Beat[] beats) {
            this.beats = new Beat[SIZE];
            System.arraycopy(beats, 0, this.beats, 0, Math.min(beats.length, SIZE));
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
            Beat beat = beats[index];
            return beat == null ? Beat.empty() : beat;
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
                switch (c) {
                    case '1' -> notes.add((byte) 0);
                    case 'q' -> notes.add((byte) 1);
                    case '2' -> notes.add((byte) 2);
                    case 'w' -> notes.add((byte) 3);
                    case '3' -> notes.add((byte) 4);
                    case 'e' -> notes.add((byte) 5);
                    case 'r' -> notes.add((byte) 6);
                    case '5' -> notes.add((byte) 7);
                    case 't' -> notes.add((byte) 8);
                    case '6' -> notes.add((byte) 9);
                    case 'y' -> notes.add((byte) 10);
                    case 'u' -> notes.add((byte) 11);
                    case '8' -> notes.add((byte) 12);
                    case 'i' -> notes.add((byte) 13);
                    case '9' -> notes.add((byte) 14);
                    case 'o' -> notes.add((byte) 15);
                    case '0' -> notes.add((byte) 16);
                    case 'p' -> notes.add((byte) 17);
                    case 'z' -> notes.add((byte) 18);
                    case 's' -> notes.add((byte) 19);
                    case 'x' -> notes.add((byte) 20);
                    case 'd' -> notes.add((byte) 21);
                    case 'c' -> notes.add((byte) 22);
                    case 'v' -> notes.add((byte) 23);
                }
            }
            return new Beat(ArrayUtils.toPrimitive(notes.toArray(new Byte[0])));
        }

        public byte[] getNotes() {
            return notes;
        }

        public boolean notEmpty() {
            return notes.length > 0;
        }

        public byte play(Level level, BlockPos blockPos, BlockState blockState) {
            for (byte note : notes) {
                level.playSeededSound(null, (double) blockPos.getX() + 0.5D, (double) blockPos.getY() + 0.5D, (double) blockPos.getZ() + 0.5D, blockState.getValue(MusicBoxBlock.INSTRUMENT).getSoundEvent(), SoundSource.RECORDS, 3.0F, NoteBlock.getPitchFromNote(note), level.random.nextLong());
            }
            return notes[0];
        }
    }
}
