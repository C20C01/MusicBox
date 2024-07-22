package io.github.c20c01.cc_mb.util.player;

import io.github.c20c01.cc_mb.data.Beat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface PlayerListener {
    /**
     * Don't forget to call {@link NoteGridPlayer#reset()}.
     */
    void onFinish(Level level, BlockPos blockPos, BlockState blockState);

    void onBeat(Level level, BlockPos blockPos, BlockState blockState, Beat lastBeat, Beat currentBeat);

    void onPageChange(Level level, BlockPos blockPos, BlockState blockState, byte pageNumber);
}
