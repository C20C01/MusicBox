package io.github.c20c01.cc_mb.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

public class EjectUtils {
    /**
     * Try to eject the item to the container first, if failed, eject it to the world.
     *
     * @param blockPos           the position where the item is ejected from.
     * @param containerDirection the direction to eject to the container, relative to the block position.
     * @param worldDirection     the direction to eject to the world, relative to the block position.
     * @param itemStack          the item stack to be ejected, should not be empty.
     */
    public static void eject(Level level, BlockPos blockPos, Direction containerDirection, Direction worldDirection, ItemStack itemStack) {
        if (!tryToContainer(level, blockPos, containerDirection, itemStack)) {
            toWorld(level, blockPos, worldDirection, itemStack);
        }
    }

    /**
     * @param blockPos  the position where the item is ejected from.
     * @param direction the direction to eject to, relative to the block position.
     * @param itemStack the item stack to be ejected, should not be empty.
     * @return true if the item is successfully ejected to the container, false otherwise.
     */
    public static boolean tryToContainer(Level level, BlockPos blockPos, Direction direction, ItemStack itemStack) {
        Container container = HopperBlockEntity.getContainerOrHandlerAt(level, blockPos.relative(direction), direction.getOpposite()).container();
        if (container == null || container instanceof WorldlyContainer) return false;

        int size = container.getContainerSize();
        for (int slot = 0; slot < size; ++slot) {
            if (container.getItem(slot).isEmpty() && container.canPlaceItem(slot, itemStack)) {
                container.setItem(slot, itemStack);
                container.setChanged();
                return true;
            }
        }
        return false;
    }

    /**
     * @param blockPos  the position where the item is ejected from.
     * @param direction the direction to eject to, relative to the block position.
     * @param itemStack the item stack to be ejected, should not be empty.
     */
    public static void toWorld(Level level, BlockPos blockPos, Direction direction, ItemStack itemStack) {
        Position position = blockPos.getCenter().relative(direction, 0.7D);
        DefaultDispenseItemBehavior.spawnItem(level, itemStack, 2, direction, position);
    }
}
