package io.github.c20c01.cc_mb.block;

import io.github.c20c01.cc_mb.block.entity.SingleItemContainerBlockEntityImpl;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;

public interface SingleItemContainerBlock {
    static InteractionResult takeOutItem(Level level, SingleItemContainerBlockEntityImpl container, Inventory inventory) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        inventory.add(container.removeItem());
        return InteractionResult.CONSUME;
    }
}
