package io.github.c20c01.cc_mb.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public class BlockUtils {
    public static <T extends Comparable<T>, V extends T> void changeProperty(Level level, BlockPos blockPos, BlockState blockState, Property<T> property, V value) {
        changeProperty(level, blockPos, blockState, property, value, Block.UPDATE_ALL);
    }

    public static <T extends Comparable<T>, V extends T> void changeProperty(Level level, BlockPos blockPos, BlockState blockState, Property<T> property, V value, int flags) {
        if (!blockState.getValue(property).equals(value)) {
            level.setBlock(blockPos, blockState.setValue(property, value), flags);
        }
    }
}
