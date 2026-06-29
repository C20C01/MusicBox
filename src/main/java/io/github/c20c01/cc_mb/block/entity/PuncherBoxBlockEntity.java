package io.github.c20c01.cc_mb.block.entity;

import io.github.c20c01.cc_mb.MusicBox;
import io.github.c20c01.cc_mb.block.PuncherBoxBlock;
import io.github.c20c01.cc_mb.data.Beat;
import io.github.c20c01.cc_mb.data.NoteGridData;
import io.github.c20c01.cc_mb.player.NoteGridIterator;
import io.github.c20c01.cc_mb.util.EjectUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class PuncherBoxBlockEntity extends NoteGridBoxBlockEntity {
    private final NoteGridIterator iterator;
    private byte power;

    public PuncherBoxBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(MusicBox.PUNCHER_BOX_BLOCK_ENTITY.get(), blockPos, blockState);
        this.iterator = new NoteGridIterator(this, this);
    }

    /**
     * Save the current note grid data to the item when taking out or saving.
     */
    @Override
    public ItemStack getItem() {
        ItemStack item = super.getItem();
        if (item.isEmpty()) return item;

        NoteGridData data = getData();
        if (data == null) return item;

        data.saveToNoteGrid(item);
        return item;
    }

    @Override
    public void setItem(ItemStack itemStack) {
        if (itemStack.isEmpty()) iterator.reset();
        super.setItem(itemStack);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        iterator.loadAdditional(input);
        super.loadAdditional(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        iterator.saveAdditional(output);
        super.saveAdditional(output);
    }

    /**
     * Trigger the puncher box with the given power when {@link PuncherBoxBlock#POWERED} is changed to true:
     * <li>1: jump to the next beat without punching</li>
     * <li>2-15: jump to the next beat and punch with the note of power-2</li>
     */
    public void trigger(int power) {
        this.power = (byte) power;
        iterator.nextBeat();
    }

    public Component getCurrentStateMessage() {
        return hasData()
                ? Component.translatable(MusicBox.TEXT_PAGE_AND_BEAT, iterator.getPageNum() + 1, iterator.getBeatNum()).withStyle(ChatFormatting.DARK_GREEN)
                : Component.translatable(MusicBox.TEXT_NO_NOTE_GRID).withStyle(ChatFormatting.RED);
    }

    @Override
    public boolean onBeat(int pageNum, int beatNum, Beat beat) {
        if (power > 1) {
            if (editNote(pageNum, beatNum, (byte) (power - 2), true) && level != null) {
                level.playSound(null, worldPosition, SoundEvents.BOOK_PUT, SoundSource.BLOCKS);
            }
            power = 1;// not necessary, just for clarity
        }
        return super.onBeat(pageNum, beatNum, beat);
    }

    @Override
    public byte getMinNote() {
        return iterator.getMinNote();
    }

    @Override
    public void ejectNoteGrid(Level level, BlockPos blockPos, BlockState blockState, ItemStack noteGrid) {
        EjectUtils.eject(level, blockPos, Direction.UP, Direction.UP, noteGrid);
    }
}
