package io.github.c20c01.cc_mb.block.entity;

import io.github.c20c01.cc_mb.MusicBox;
import io.github.c20c01.cc_mb.block.NoteGridBoxBlock;
import io.github.c20c01.cc_mb.data.Beat;
import io.github.c20c01.cc_mb.data.NoteGridData;
import io.github.c20c01.cc_mb.inventory.SingleItemContainer;
import io.github.c20c01.cc_mb.player.NoteGridDataHolder;
import io.github.c20c01.cc_mb.player.NoteGridIteratorListener;
import io.github.c20c01.cc_mb.util.BlockUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import javax.annotation.Nullable;

public abstract class NoteGridBoxBlockEntity extends BlockEntity implements SingleItemContainer.SingleItemContainerBlockEntity, NoteGridDataHolder, NoteGridIteratorListener {
    private ItemStack noteGrid = ItemStack.EMPTY;

    @Nullable
    private NoteGridData data;

    public NoteGridBoxBlockEntity(BlockEntityType<?> type, BlockPos worldPosition, BlockState blockState) {
        super(type, worldPosition, blockState);
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
    protected void loadAdditional(ValueInput input) {
        input.read("note_grid", ItemStack.CODEC).ifPresent(this::setItem);
        super.loadAdditional(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        if (!noteGrid.isEmpty()) output.store("note_grid", ItemStack.CODEC, getItem());
        super.saveAdditional(output);
    }

    @Override
    public BlockEntity getContainerBlockEntity() {
        return this;
    }

    @Override
    public ItemStack getItem() {
        return noteGrid;
    }

    @Override
    public void setItem(ItemStack itemStack) {
        this.noteGrid = itemStack;
        boolean hasNoteGrid = !itemStack.isEmpty();
        this.data = hasNoteGrid ? NoteGridData.ofNoteGrid(itemStack) : null;
        if (level != null) {
            BlockUtils.changeProperty(level, worldPosition, getBlockState(), NoteGridBoxBlock.HAS_NOTE_GRID, hasNoteGrid);
            setChanged(level, worldPosition, getBlockState());
        }
    }

    public boolean canPlaceItem(ItemStack itemStack) {
        return noteGrid.isEmpty() && itemStack.is(MusicBox.NOTE_GRID_ITEM.get());
    }

    public boolean canPlaceItem(int slot, ItemStack itemStack) {
        return slot == 0 && canPlaceItem(itemStack);
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

    public void setData(@Nullable NoteGridData data) {
        this.data = data;
    }

    @Override
    public int getDataSize() {
        return data == null ? 0 : data.size();
    }

    @Override
    public Beat getBeat(int pageNum, int beatNum) {
        if (data == null || data.size() <= pageNum) {
            return Beat.EMPTY;
        }
        return data.getPage(pageNum).getBeat(beatNum);
    }

    @Override
    public boolean onBeat(int pageNum, int beatNum, Beat beat) {
        setChanged();
        return true;
    }

    @Override
    public void onPageChanged(int pageNum) {
        // do nothing by default
    }

    @Override
    public void onFinish() {
        if (level instanceof ServerLevel) {
            ItemStack noteGrid = removeItem();
            if (!noteGrid.isEmpty()) {
                ejectNoteGrid(level, worldPosition, getBlockState(), noteGrid);
            }
        }
    }
}
