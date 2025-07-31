package io.github.c20c01.cc_mb.util.player;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public abstract class BaseBoxPlayer extends BasePlayer {
    protected final Listener listener;
    protected Level level;
    protected BlockPos blockPos;
    protected BlockState blockState;

    public BaseBoxPlayer(Listener listener) {
        this.listener = listener;
    }

    @Override
    protected void onPageChange() {
        listener.onPageChange(level, blockPos);
    }

    @Override
    protected void onFinish() {
        listener.onFinish(level, blockPos, blockState);
    }

    @Override
    protected boolean shouldPause() {
        return false;
    }

    public void update(Level level, BlockPos blockPos, BlockState blockState) {
        this.level = level;
        this.blockPos = blockPos;
        this.blockState = blockState;
    }

    public void nextBeat(Level level, BlockPos blockPos, BlockState blockState) {
        update(level, blockPos, blockState);
        nextBeat();
    }

    public byte getMinNote() {
        return currentBeat.getMinNote();
    }

    public void load(CompoundTag tag) {
        beatNumber = tag.getByte("beat");
        pageNumber = tag.getByte("page");
    }

    public void saveAdditional(CompoundTag tag) {
        tag.putByte("beat", beatNumber);
        tag.putByte("page", pageNumber);
    }

    public interface Listener {
        void onBeat();

        void onPageChange(Level level, BlockPos blockPos);

        void onFinish(Level level, BlockPos blockPos, BlockState blockState);
    }
}
