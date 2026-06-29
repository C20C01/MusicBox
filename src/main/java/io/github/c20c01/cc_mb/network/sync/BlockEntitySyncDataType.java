package io.github.c20c01.cc_mb.network.sync;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public abstract class BlockEntitySyncDataType<T extends BlockEntity> {
    protected final int bitMask;

    public BlockEntitySyncDataType(int index) {
        if (index < 0 || index > 31) {
            throw new IllegalArgumentException("Index must be between 0 and 31");
        }
        this.bitMask = 1 << index;
    }

    public abstract void writeData(T blockEntity, ValueOutput output);

    public abstract void readData(T blockEntity, ValueInput input);
}
