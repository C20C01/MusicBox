package io.github.c20c01.cc_mb.block;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.util.NoteGridData;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.ContainerSingleItem;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault

public class MusicBoxBlockEntity extends BlockEntity implements ContainerSingleItem {
    public static final int MIN_TICK_PER_BEAT = 2;
    public static final int MAX_TICK_PER_BEAT = 20;
    private byte delta = 0;
    private byte beat = 0;
    private byte page = 0;
    private byte note = -1;
    private byte lastNote = -2;
    private byte tickPerBeat = 10;
    private NoteGridData.Page[] pages;
    private ItemStack noteGrid = ItemStack.EMPTY;

    public MusicBoxBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(CCMain.MUSIC_BOX_BLOCK_ENTITY.get(), blockPos, blockState);
    }

    public boolean setNoteGrid(ItemStack noteGrid) {
        if (this.noteGrid.isEmpty()) {
            this.noteGrid = noteGrid.copy();
            pages = NoteGridData.readFromTag(this.noteGrid);
            if (getLevel() != null) {
                CCUtil.changeProperty(getLevel(), getBlockPos(), getBlockState(), MusicBoxBlock.EMPTY, Boolean.FALSE);
            }
            setChanged();
            return true;
        }
        return false;
    }

    private void reset() {
        delta = 0;
        beat = 0;
        page = 0;
        note = -1;
        lastNote = -2;
        pages = null;
        noteGrid = ItemStack.EMPTY;
        setChanged();
    }

    public ItemStack outNoteGrid() {
        if (noteGrid.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack oldItemStack = noteGrid.copy();
        if (getLevel() != null)
            CCUtil.changeProperty(getLevel(), getBlockPos(), getBlockState(), MusicBoxBlock.EMPTY, Boolean.TRUE);
        reset();
        return oldItemStack;
    }

    public int getAnalogOutputSignal() {
        return note > 13 ? 15 : note + 2;
    }

    public static void playTick(Level level, BlockPos blockPos, BlockState blockState, MusicBoxBlockEntity blockEntity) {
        if (blockState.getValue(MusicBoxBlock.POWERED) && !blockState.getValue(MusicBoxBlock.EMPTY)) {
            blockEntity.playTick((ServerLevel) level, blockPos, blockState);
        }
    }

    private void playTick(ServerLevel level, BlockPos blockPos, BlockState blockState) {
        if (pages == null) return;
        delta++;
        if (delta >= tickPerBeat) {
            delta = 0;
            tryToPlayOneBeat(level, blockPos, blockState);
        }
    }

    public void tryToPlayOneBeat(ServerLevel level, BlockPos blockPos, BlockState blockState) {
        if (pages == null) return;

        try {
            NoteGridData.Page nowPage = pages[page];
            NoteGridData.Beat oneBeat = nowPage.getBeat(beat);
            note = playOneBeat(oneBeat, level, blockPos, blockState);
        } catch (ArrayIndexOutOfBoundsException e) {
            finishOneNoteGrid(level, blockPos, blockState);
            return;
        }

        beat++;
        if (beat >= NoteGridData.Page.SIZE) {
            page++;
            if (page >= pages.length) {
                finishOneNoteGrid(level, blockPos, blockState);
            } else {
                beat = 0;
            }
        }

        if (note != lastNote) {
            lastNote = note;
            level.updateNeighbourForOutputSignal(blockPos, blockState.getBlock());
        }

        setChanged();
    }

    private byte playOneBeat(NoteGridData.Beat beat, ServerLevel level, BlockPos blockPos, BlockState blockState) {
        if (beat.notEmpty()) {
            NoteBlockInstrument instrument = blockState.getValue(MusicBoxBlock.INSTRUMENT);
            Holder<SoundEvent> holder;
            long soundSeed;

            if (instrument.hasCustomSound() && level.getBlockEntity(blockPos.below()) instanceof SoundBoxBlockEntity soundBoxBlockEntity) {
                // 按声响盒的声音播放
                holder = soundBoxBlockEntity.getInstrument();
                if (holder == SoundBoxBlockEntity.EMPTY) {
                    return beat.getMinNote();
                }

                // 规定种子是为了保证每次播放的都是声音事件里的同一种声音
                // 这应该是在不mixin的情况下最方便的法子了
                soundSeed = soundBoxBlockEntity.getSoundSeed();
            } else {
                holder = instrument.getSoundEvent();
                soundSeed = level.random.nextLong();
            }

            byte minNote = beat.play(level, blockPos, holder, soundSeed);

            if (level.getBlockState(blockPos.above()).isAir()) {
                spawnMusicParticles(level, blockPos, minNote);
            }

            return minNote;
        }
        return -1;
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
        page = compoundTag.getByte("Page");
        note = compoundTag.getByte("Note");
        setTickPerBeat(compoundTag.getByte("TickPerBeat"));
        ItemStack noteGrid = ItemStack.of(compoundTag.getCompound("NoteGrid"));
        if (noteGrid.is(CCMain.NOTE_GRID_ITEM.get())) setNoteGrid(noteGrid);
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag) {
        super.saveAdditional(compoundTag);
        compoundTag.putByte("Delta", delta);
        compoundTag.putShort("Beat", beat);
        compoundTag.putByte("Page", page);
        compoundTag.putByte("Note", note);
        compoundTag.putByte("TickPerBeat", tickPerBeat);
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
        return false;
    }

    private void spawnMusicParticles(Level level, BlockPos blockPos, byte note) {
        if (level instanceof ServerLevel serverlevel) {
            Vec3 vec3 = Vec3.atBottomCenterOf(blockPos).add(0.0D, 1.2F, 0.0D);
            serverlevel.sendParticles(ParticleTypes.NOTE, vec3.x(), vec3.y(), vec3.z(), 0, note / 24D, 0.0D, 0.0D, 1.0D);
        }
    }

    public void setTickPerBeat(byte tickPerBeat) {
        if (tickPerBeat < MIN_TICK_PER_BEAT) {
            this.tickPerBeat = MIN_TICK_PER_BEAT;
            return;
        }
        if (tickPerBeat > MAX_TICK_PER_BEAT) {
            this.tickPerBeat = MAX_TICK_PER_BEAT;
            return;
        }
        if (this.tickPerBeat != tickPerBeat) {
            this.tickPerBeat = tickPerBeat;
            setChanged();
        }
    }

    public byte getTickPerBeat() {
        return tickPerBeat;
    }
}