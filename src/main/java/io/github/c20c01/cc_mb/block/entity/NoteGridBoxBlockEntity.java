package io.github.c20c01.cc_mb.block.entity;

import io.github.c20c01.cc_mb.MusicBox;
import io.github.c20c01.cc_mb.block.NoteGridBoxBlock;
import io.github.c20c01.cc_mb.data.Beat;
import io.github.c20c01.cc_mb.data.NoteGridData;
import io.github.c20c01.cc_mb.player.NoteGridDataHolder;
import io.github.c20c01.cc_mb.player.NoteGridIteratorListener;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public abstract class NoteGridBoxBlockEntity extends SingleItemContainerBlockEntityImpl implements NoteGridDataHolder, NoteGridIteratorListener {
    @Nullable
    private NoteGridData data;

    public NoteGridBoxBlockEntity(BlockEntityType<?> type, BlockPos worldPosition, BlockState blockState) {
        super(type, worldPosition, blockState, "note_grid", NoteGridBoxBlock.HAS_NOTE_GRID);
    }

    /**
     * @return the minimum note in current beat.
     */
    public abstract byte getMinNote();

    /**
     * Eject the note grid item when finished.
     *
     * @param noteGrid should not be empty.
     */
    public abstract void ejectNoteGrid(Level level, BlockPos blockPos, BlockState blockState, ItemStack noteGrid);

    @Override
    public void setItem(ItemStack itemStack) {
        this.data = itemStack.isEmpty() ? null : NoteGridData.ofNoteGrid(itemStack);
        super.setItem(itemStack);
    }

    @Override
    public boolean canPlaceItem(ItemStack itemStack) {
        return isEmpty() && itemStack.is(MusicBox.NOTE_GRID_ITEM.get());
    }

    @Override
    public boolean canTakeItem(Container target, int index, ItemStack itemStack) {
        return target.hasAnyMatching(ItemStack::isEmpty) && !getBlockState().getValue(NoteGridBoxBlock.POWERED);
    }

    @Override
    @Nullable
    public NoteGridData getData() {
        return data;
    }

    @Override
    public void setData(@Nullable NoteGridData data) {
        this.data = data;
    }

    @Override
    public boolean onBeat(int pageNum, int beatNum, Beat beat) {
        setChanged();
        return true;
    }

    @Override
    public void onPageChanged() {
        // do nothing by default
    }

    @Override
    public void onFinished() {
        if (level instanceof ServerLevel) {
            ItemStack noteGrid = removeItem();
            if (!noteGrid.isEmpty()) {
                ejectNoteGrid(level, worldPosition, getBlockState(), noteGrid);
            }
        }
    }
}
