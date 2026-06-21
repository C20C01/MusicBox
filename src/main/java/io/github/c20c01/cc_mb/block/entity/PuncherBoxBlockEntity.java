package io.github.c20c01.cc_mb.block.entity;

import io.github.c20c01.cc_mb.MusicBox;
import io.github.c20c01.cc_mb.block.PuncherBoxBlock;
import io.github.c20c01.cc_mb.data.Beat;
import io.github.c20c01.cc_mb.data.NoteGridData;
import io.github.c20c01.cc_mb.player.NoteGridIterator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
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
        super.setItem(itemStack);
        if (getData() == null) iterator.reset();
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
    public void trigger(byte power) {
        this.power = power;
        iterator.nextBeat();
    }

    @Override
    public boolean onBeat(int pageNum, int beatNum, Beat beat) {
        if (power > 1) {
            NoteGridData data = getData();
            if (data != null) {
                byte note = (byte) (power - 2);// [0, 13]
                NoteGridData edited = data.withNoteChanged(pageNum, beatNum, note, true);
                if (edited != null) {
                    setData(edited);
                    if (level != null) {
                        level.playSound(null, worldPosition, SoundEvents.BOOK_PUT, SoundSource.BLOCKS);
                    }
                }
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
        // TODO 能不能让它在一定条件下提前丢出？ & 补全与容器的交互
        Position position = blockPos.getCenter().relative(Direction.UP, 0.7D);
        DefaultDispenseItemBehavior.spawnItem(level, noteGrid, 2, Direction.UP, position);
    }

    @Override
    public boolean canTakeItem(Container target, int index, ItemStack itemStack) {
        // TODO 能不能设计些机制让它能被拿走？
        return false;
    }
}
