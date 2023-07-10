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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Arrays;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault

public class NoteGrid extends Item {
    public NoteGrid() {
        super(new Properties().stacksTo(1));
    }

    public static ItemStack changeToTestingGrid(ItemStack noteGrid) {
        saveToTag(noteGrid, new Page[]{
                new Page(new Beat[]{
                        new Beat(new byte[]{6}), new Beat(new byte[]{6}), new Beat(new byte[]{13}), new Beat(new byte[]{13}), new Beat(new byte[]{15}), new Beat(new byte[]{15}), new Beat(new byte[]{13}), Beat.empty(),
                        new Beat(new byte[]{11}), new Beat(new byte[]{11}), new Beat(new byte[]{10}), new Beat(new byte[]{10}), new Beat(new byte[]{8}), new Beat(new byte[]{8}), new Beat(new byte[]{6}), Beat.empty(),
                        new Beat(new byte[]{13}), new Beat(new byte[]{13}), new Beat(new byte[]{11}), new Beat(new byte[]{11}), new Beat(new byte[]{10}), new Beat(new byte[]{10}), new Beat(new byte[]{8}), Beat.empty(),
                        new Beat(new byte[]{13}), new Beat(new byte[]{13}), new Beat(new byte[]{11}), new Beat(new byte[]{11}), new Beat(new byte[]{10}), new Beat(new byte[]{10}), new Beat(new byte[]{8}), Beat.empty(),
                        new Beat(new byte[]{6}), new Beat(new byte[]{6}), new Beat(new byte[]{13}), new Beat(new byte[]{13}), new Beat(new byte[]{15}), new Beat(new byte[]{15}), new Beat(new byte[]{13}), Beat.empty(),
                        new Beat(new byte[]{11}), new Beat(new byte[]{11}), new Beat(new byte[]{10}), new Beat(new byte[]{10}), new Beat(new byte[]{8}), new Beat(new byte[]{8}), new Beat(new byte[]{6}), Beat.empty()
                }),
                new Page(new Beat[]{
                        new Beat(new byte[]{0, 24}), new Beat(new byte[]{1}), new Beat(new byte[]{2}), new Beat(new byte[]{3}), new Beat(new byte[]{4}), new Beat(new byte[]{5}), new Beat(new byte[]{6}), new Beat(new byte[]{7}),
                        new Beat(new byte[]{8}), new Beat(new byte[]{9}), new Beat(new byte[]{10}), new Beat(new byte[]{11}), new Beat(new byte[]{12}), new Beat(new byte[]{13}), new Beat(new byte[]{14}), new Beat(new byte[]{15}),
                        new Beat(new byte[]{16}), new Beat(new byte[]{17}), new Beat(new byte[]{18}), new Beat(new byte[]{19}), new Beat(new byte[]{20}), new Beat(new byte[]{21}), new Beat(new byte[]{22}), new Beat(new byte[]{0, 23}),
                        new Beat(new byte[]{0, 23}), new Beat(new byte[]{1}), new Beat(new byte[]{2}), new Beat(new byte[]{3}), new Beat(new byte[]{4}), new Beat(new byte[]{5}), new Beat(new byte[]{6}), new Beat(new byte[]{7}),
                        new Beat(new byte[]{8}), new Beat(new byte[]{9}), new Beat(new byte[]{10}), new Beat(new byte[]{11}), new Beat(new byte[]{12}), new Beat(new byte[]{13}), new Beat(new byte[]{14}), new Beat(new byte[]{15}),
                        new Beat(new byte[]{16}), new Beat(new byte[]{17}), new Beat(new byte[]{18}), new Beat(new byte[]{19}), new Beat(new byte[]{20}), new Beat(new byte[]{21}), new Beat(new byte[]{22}), new Beat(new byte[]{0, 23}),
                        new Beat(new byte[]{0, 23}), new Beat(new byte[]{1}), new Beat(new byte[]{2}), new Beat(new byte[]{3}), new Beat(new byte[]{4}), new Beat(new byte[]{5}), new Beat(new byte[]{6}), new Beat(new byte[]{7}),
                        new Beat(new byte[]{8}), new Beat(new byte[]{9}), new Beat(new byte[]{10}), new Beat(new byte[]{11}), new Beat(new byte[]{12}), new Beat(new byte[]{13}), new Beat(new byte[]{14}), new Beat(new byte[]{0, 15, 23})
                })
        });
        noteGrid.setHoverName(Component.literal("Testing Note Grid"));
        return noteGrid;
    }

//    @Override
//    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
//        ItemStack itemStack = player.getItemInHand(hand);
//        if (!level.isClientSide) {
//            if (player.isShiftKeyDown()) {
//                changeToTestingGrid(itemStack);
//            } else {
//                CCMain.LOGGER.debug(Arrays.toString(readFromTag(itemStack)));
//            }
//        }
//        return super.use(level, player, hand);
//    }

    public static void saveToTag(ItemStack noteGrid, Page[] pages) {
        if (pages.length == 0) return;
        ListTag pagesTag = new ListTag();
        for (int i = 0; i < Math.min(pages.length, 64); i++) {
            // 最多有64页
            pagesTag.add(pages[i].toTag());
        }
        noteGrid.getOrCreateTag().put("Notes", pagesTag);
    }

    public static Page[] readFromTag(ItemStack noteGrid) {
        CompoundTag tag = noteGrid.getOrCreateTag();
        if (tag.contains("Notes")) {
            ArrayList<Page> pages = new ArrayList<>();
            for (Tag pageTag : tag.getList("Notes", Tag.TAG_LIST)) {
                pages.add(Page.of((ListTag) pageTag));
            }
            return pages.toArray(new Page[0]);
        }

        return new Page[]{new Page()};
    }

    public static Page[] connect(ItemStack noteGrid, ItemStack other) {
        return ArrayUtils.addAll(readFromTag(noteGrid), readFromTag(other));
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

        public Beat getBeat(byte index) throws ArrayIndexOutOfBoundsException {
            Beat beat = beats[index];
            return beat == null ? Beat.empty() : beat;
        }
    }

    public static class Beat {
        private byte[] notes;

        public Beat(byte[] notes) {
            this.notes = notes;
        }

        public static Beat empty() {
            return new Beat(new byte[0]);
        }

        @Override
        public String toString() {
            return "Beat:" + Arrays.toString(notes);
        }

        public boolean addNote(byte note) {
            if (note < 0 || note > 24) return false;
            if (ArrayUtils.contains(notes, note)) return false;
            notes = ArrayUtils.add(notes, note);
            Arrays.sort(notes);
            return true;
        }

        public ByteArrayTag toTag() {
            return new ByteArrayTag(notes);
        }

        public static Beat of(Tag beatTag) {
            return new Beat(((ByteArrayTag) beatTag).getAsByteArray());
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
