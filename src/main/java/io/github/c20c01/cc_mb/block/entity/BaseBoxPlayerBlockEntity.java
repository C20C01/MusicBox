package io.github.c20c01.cc_mb.block.entity;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.block.BaseBoxPlayerBlock;
import io.github.c20c01.cc_mb.data.NoteGridData;
import io.github.c20c01.cc_mb.util.BlockUtils;
import io.github.c20c01.cc_mb.util.player.BaseBoxPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public abstract class BaseBoxPlayerBlockEntity<T extends BaseBoxPlayer> extends AbstractItemLoaderBlockEntity implements BaseBoxPlayer.Listener {
    public static final String NOTE_GRID = "note_grid";

    public BaseBoxPlayerBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState, NOTE_GRID);
    }

    abstract T getPlayer();

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        super.loadAdditional(tag, lookupProvider);
        getPlayer().load(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        super.saveAdditional(tag, lookupProvider);
        getPlayer().saveAdditional(tag);
    }

    @Override
    protected void loadItem(ItemStack noteGrid) {
        getPlayer().data = NoteGridData.ofNoteGrid(noteGrid);
        if (level != null) {
            BlockUtils.changeProperty(level, getBlockPos(), getBlockState(), BaseBoxPlayerBlock.HAS_NOTE_GRID, true);
        }
    }

    @Override
    protected void unloadItem() {
        getPlayer().reset();
        if (level != null) {
            BlockUtils.changeProperty(level, getBlockPos(), getBlockState(), BaseBoxPlayerBlock.HAS_NOTE_GRID, false);
        }
    }

    @Override
    public boolean canPlaceItem(ItemStack itemStack) {
        return itemStack.is(CCMain.NOTE_GRID_ITEM.get());
    }

    @Override
    public boolean canTakeItem(Container target, int index, ItemStack itemStack) {
        return target.hasAnyMatching(ItemStack::isEmpty) && !getBlockState().getValue(BaseBoxPlayerBlock.POWERED);
    }

    /**
     * Eject the note grid item from the music box.
     * If there is a container(NOT a worldly container) at the back of the music box, put the note grid item into it.
     * Otherwise, spawn the note grid item.
     */
    public void ejectNoteGrid(Level level, BlockPos blockPos, BlockState blockState) {
        Direction direction = blockState.getValue(BaseBoxPlayerBlock.FACING);
        Container container = HopperBlockEntity.getContainerAt(level, blockPos.relative(direction.getOpposite()));
        ItemStack itemStack = removeItem();
        if (container != null && !(container instanceof WorldlyContainer)) {
            int size = container.getContainerSize();
            for (int slot = 0; slot < size; ++slot) {
                if (container.getItem(slot).isEmpty()) {
                    container.setItem(slot, itemStack);
                    container.setChanged();
                    return;
                }
            }
        }
        Position position = blockPos.getCenter().relative(direction, 0.7D);
        DefaultDispenseItemBehavior.spawnItem(level, itemStack, 2, direction, position);
    }

    public int getSignal() {
        byte minNote = getPlayer().getMinNote();
        return minNote > 13 ? 15 : minNote + 2;
    }

    @Override
    public void onFinish(Level level, BlockPos blockPos, BlockState blockState) {
        if (!level.isClientSide) {
            ejectNoteGrid(level, blockPos, blockState);
        }
    }

    @Override
    public void onBeat() {
        setChanged();
    }

    @Override
    public void onPageChange(Level level, BlockPos blockPos) {
        // do nothing by default
    }
}
