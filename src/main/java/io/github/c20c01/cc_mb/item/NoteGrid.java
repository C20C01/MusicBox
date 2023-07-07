package io.github.c20c01.cc_mb.item;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.block.MusicBoxBlock;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault

public class NoteGrid extends Item {
    public NoteGrid() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                saveToTag(itemStack, new Beat[]{new Beat(new byte[]{8}), new Beat(new byte[]{8}), new Beat(new byte[]{15}), new Beat(new byte[]{15}), new Beat(new byte[]{17}), new Beat(new byte[]{17}), new Beat(new byte[]{15}), null, new Beat(new byte[]{13}), new Beat(new byte[]{13}), new Beat(new byte[]{12}), new Beat(new byte[]{12}), new Beat(new byte[]{10}), new Beat(new byte[]{10}), new Beat(new byte[]{8}), null, new Beat(new byte[]{15}), new Beat(new byte[]{15}), new Beat(new byte[]{13}), new Beat(new byte[]{13}), new Beat(new byte[]{12}), new Beat(new byte[]{12}), new Beat(new byte[]{10}), null, new Beat(new byte[]{15}), new Beat(new byte[]{15}), new Beat(new byte[]{13}), new Beat(new byte[]{13}), new Beat(new byte[]{12}), new Beat(new byte[]{12}), new Beat(new byte[]{10}), null, new Beat(new byte[]{8}), new Beat(new byte[]{8}), new Beat(new byte[]{15}), new Beat(new byte[]{15}), new Beat(new byte[]{17}), new Beat(new byte[]{17}), new Beat(new byte[]{15}), null, new Beat(new byte[]{13}), new Beat(new byte[]{13}), new Beat(new byte[]{12}), new Beat(new byte[]{12}), new Beat(new byte[]{10}), new Beat(new byte[]{10}), new Beat(new byte[]{8}), null});
            } else {
                CCMain.LOGGER.info(Arrays.toString(readFromTag(itemStack)));
            }
        }

        return super.use(level, player, hand);
    }

//    @Override
//    public InteractionResult useOn(UseOnContext context) {
//        ItemStack itemStack = context.getItemInHand();
//        Level level = context.getLevel();
//        BlockPos blockPos = context.getClickedPos();
//
//        if (level.getBlockEntity(blockPos) instanceof MusicBoxBlockEntity blockEntity && blockEntity.setNoteGrid(itemStack)) {
//            level.setBlockAndUpdate(blockPos, level.getBlockState(blockPos).setValue(MusicBoxBlock.EMPTY, Boolean.FALSE));
//            return InteractionResult.sidedSuccess(level.isClientSide);
//        }
//        return super.useOn(context);
//    }

    public static void saveToTag(ItemStack grid, Beat[] beats) {
        ListTag beatTags = new ListTag();
        int length = Math.min(beats.length, 128);
        for (int i = 0; i < length; i++) {
            Beat beat = beats[i];
            if (beat == null) continue;
            beatTags.add(beat.toTag((byte) i));
        }
        CompoundTag tag = grid.getOrCreateTag();
        tag.putInt("Length", length);
        tag.put("Notes", beatTags);
    }

    public static Beat[] readFromTag(ItemStack grid) {
        CompoundTag tag = grid.getOrCreateTag();
        int length = tag.contains("Length") ? Math.min(tag.getInt("Length"), 128) : 128;
        Beat[] beats = new Beat[length];


        if (tag.contains("Notes")) {
            for (Tag beatTag : tag.getList("Notes", Tag.TAG_BYTE_ARRAY)) {
                byte[] array = ((ByteArrayTag) beatTag).getAsByteArray();
                byte index = array[0];
                if (index >= length) break;
                beats[index] = Beat.fromTag(array);
            }
        }

        return beats;
    }

    public static class Beat {
        private byte[] notes;

        public Beat(byte[] notes) {
            this.notes = notes;
        }

        @Override
        public String toString() {
            return "Beat:" + Arrays.toString(notes);
        }

        public void addNote(byte note) {
            notes = ArrayUtils.add(notes, note);
        }

        public ByteArrayTag toTag(byte index) {
            return new ByteArrayTag(ArrayUtils.insert(0, notes, index));
        }

        public static Beat fromTag(byte[] arrayFromTag) {
            byte[] temp = new byte[arrayFromTag.length - 1];
            System.arraycopy(arrayFromTag, 1, temp, 0, temp.length);
            return new Beat(temp);
        }

        public byte[] getNotes() {
            return notes;
        }

        public byte test(Level level, BlockPos blockPos, BlockState blockState) {
            for (byte note : notes) {
                level.playSeededSound(null, (double) blockPos.getX() + 0.5D, (double) blockPos.getY() + 0.5D, (double) blockPos.getZ() + 0.5D, blockState.getValue(MusicBoxBlock.INSTRUMENT).getSoundEvent(), SoundSource.RECORDS, 3.0F, NoteBlock.getPitchFromNote(note), level.random.nextLong());
            }
            return notes[0];
        }
    }
}
