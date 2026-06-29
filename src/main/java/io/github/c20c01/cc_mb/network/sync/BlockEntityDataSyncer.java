package io.github.c20c01.cc_mb.network.sync;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public abstract class BlockEntityDataSyncer<T extends BlockEntity> {
    private int dirtyMask = 0;

    public static CompoundTag getUpdateTag(ProblemReporter.PathElement root, Logger logger, HolderLookup.Provider registries, Consumer<TagValueOutput> tagWriter) {
        CompoundTag tag;
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(root, logger)) {
            TagValueOutput output = TagValueOutput.createWithContext(reporter, registries);
            tagWriter.accept(output);
            tag = output.buildResult();
        }
        return tag;
    }

    /**
     * @return readonly list of all BlockEntitySyncDataType for this BlockEntity type
     */
    public abstract List<BlockEntitySyncDataType<T>> getAllTypes();

    public void markDirty(BlockEntitySyncDataType<T> type) {
        dirtyMask |= type.bitMask;
    }

    public void flush(T blockEntity) {
        if (dirtyMask == 0) return;
        if (blockEntity.getLevel() instanceof ServerLevel serverLevel) {
            serverLevel.getChunkSource().blockChanged(blockEntity.getBlockPos());
        }
    }

    public void sync(T blockEntity, BlockEntitySyncDataType<T> type) {
        markDirty(type);
        flush(blockEntity);
    }

    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket(T blockEntity, Logger logger) {
        return dirtyMask == 0 ? null : ClientboundBlockEntityDataPacket.create(blockEntity, (be, registries) ->
                getUpdateTag(be.problemPath(), logger, registries, output -> {
                    output.putInt("flag", dirtyMask);
                    for (BlockEntitySyncDataType<T> type : getAllTypes()) {
                        if ((dirtyMask & type.bitMask) != 0) type.writeData(blockEntity, output);
                    }
                    dirtyMask = 0;
                }));
    }

    public void handleUpdatePacket(T blockEntity, ValueInput input) {
        input.getInt("flag").ifPresent(flag -> {
            for (BlockEntitySyncDataType<T> type : getAllTypes()) {
                if ((flag & type.bitMask) != 0) type.readData(blockEntity, input);
            }
        });
    }
}
