package io.github.c20c01.cc_mb.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public class CCUtil {
    protected static <T extends Comparable<T>, V extends T> void changeProperty(Level level, BlockPos blockPos, BlockState blockState, Property<T> property, V value) {
        if (!blockState.getValue(property).equals(value)) {
            level.setBlockAndUpdate(blockPos, blockState.setValue(property, value));
        }
    }

}
