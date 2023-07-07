package io.github.c20c01.cc_mb.block;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.item.NoteGrid;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.ContainerSingleItem;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault

public class MusicBoxBlockEntity extends BlockEntity implements ContainerSingleItem {
    private byte delta = 0;
    private byte beat = 0;
    private NoteGrid.Beat[] beats;
    private ItemStack noteGrid = ItemStack.EMPTY;

    public MusicBoxBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(CCMain.MUSIC_BOX_BLOCK_ENTITY.get(), blockPos, blockState);
    }

    public boolean setNoteGrid(ItemStack noteGrid) {
        if (this.noteGrid.isEmpty()) {
            this.noteGrid = noteGrid.copy();
            beats = NoteGrid.readFromTag(this.noteGrid);
            if (getLevel() != null)
                MusicBoxBlock.changeProperty(getLevel(), getBlockPos(), getBlockState(), MusicBoxBlock.EMPTY, Boolean.FALSE);
            return true;
        }
        return false;
    }

    private void reset() {
        delta = 0;
        beat = 0;
        beats = null;
        noteGrid = ItemStack.EMPTY;
    }

    public ItemStack getNoteGrid() {
        return noteGrid.copy();
    }

    public ItemStack outNoteGrid() {
        if (noteGrid.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack oldItemStack = noteGrid.copy();
        if (getLevel() != null)
            MusicBoxBlock.changeProperty(getLevel(), getBlockPos(), getBlockState(), MusicBoxBlock.EMPTY, Boolean.TRUE);
        reset();
        return oldItemStack;
    }

    public static void playTick(Level level, BlockPos blockPos, BlockState blockState, MusicBoxBlockEntity blockEntity) {
        if (blockState.getValue(MusicBoxBlock.POWERED)) {
            blockEntity.playTick((ServerLevel) level, blockPos, blockState);
        }
    }

    public void playTick(ServerLevel level, BlockPos blockPos, BlockState blockState) {
        delta++;
        if (delta >= 10) {
            delta = 0;
            playOneBeat(level, blockPos, blockState);
        }
    }

    public void playOneBeat(ServerLevel level, BlockPos blockPos, BlockState blockState) {
        if (beats == null) return;
        if (this.beat > -1 && this.beat < beats.length) {
            NoteGrid.Beat beat = beats[this.beat];
            if (beat != null) {
                byte note = beat.test(level, blockPos, blockState);
                if (level.getBlockState(blockPos.above()).isAir()) spawnMusicParticles(level, blockPos, note);
            }
            this.beat++;
        } else {
            finishOneNoteGrid(level, blockPos, blockState);
        }
    }

    private void finishOneNoteGrid(ServerLevel level, BlockPos blockPos, BlockState blockState) {
        // 把用完的纸带往八音盒后面的容器存 不能就向前弹出来
        Direction direction = blockState.getValue(MusicBoxBlock.FACING);
        Container container = HopperBlockEntity.getContainerAt(level, blockPos.relative(direction.getOpposite()));
        ItemStack itemStack = outNoteGrid();

        if (container != null && !(container instanceof WorldlyContainer)) {
            int size = container.getContainerSize();
            for (int slot = 0; slot < size; ++slot) {
                if (container.getItem(slot).isEmpty()) {
                    container.setItem(slot, itemStack);
                    return;
                }
            }
        }

        Position position = blockPos.getCenter().relative(direction, 0.7D);
        DefaultDispenseItemBehavior.spawnItem(level, itemStack, 2, direction, position);
    }

    @Override
    public void load(CompoundTag compoundTag) {
        super.load(compoundTag);
        delta = compoundTag.getByte("Delta");
        beat = compoundTag.getByte("Beat");
        setNoteGrid(ItemStack.of(compoundTag.getCompound("NoteGrid")));
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag) {
        super.saveAdditional(compoundTag);
        compoundTag.putByte("Delta", delta);
        compoundTag.putByte("Beat", beat);
        compoundTag.put("NoteGrid", noteGrid.save(new CompoundTag()));
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == 0 ? noteGrid : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return slot == 0 ? outNoteGrid() : ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack itemStack) {
        if (slot != 0) return;

        if (itemStack.isEmpty()) {
            outNoteGrid();
        } else if (itemStack.is(CCMain.NOTE_GRID_ITEM.get())) {
            setNoteGrid(itemStack);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack itemStack) {
        return itemStack.is(CCMain.NOTE_GRID_ITEM.get()) && this.getItem(slot).isEmpty();
    }

    @Override
    public boolean canTakeItem(Container container, int slot, ItemStack itemStack) {
        CCMain.LOGGER.info(itemStack.toString());
        return false;
    }

    private void spawnMusicParticles(Level level, BlockPos blockPos, byte note) {
        if (level instanceof ServerLevel serverlevel) {
            Vec3 vec3 = Vec3.atBottomCenterOf(blockPos).add(0.0D, 1.2F, 0.0D);
            serverlevel.sendParticles(ParticleTypes.NOTE, vec3.x(), vec3.y(), vec3.z(), 0, note / 24D, 0.0D, 0.0D, 1.0D);
        }
    }
}