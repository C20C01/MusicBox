package io.github.c20c01.cc_mb.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.extensions.IForgeBlockEntity;

/**
 * A block entity that can sync data lazily. This is useful when the data is large and not always changed.
 * <p>
 * The existing sync methods in BE will send the data whenever the block is changed by setBlock, with no judgement.
 * In this mod, the note grid data is large and not always changed, but the block state can be changed frequently.
 * So it is not wise to sync the data every time the block state is changed.
 * <p>
 * With this interface, we will only sync the variable data in existing ways, and sync the large data only when requested.
 * The request will be sent when the client {@link BlockEntity#handleUpdateTag(CompoundTag) handleUpdateTag} and {@link #shouldRequestLazyUpdate()} returns true.
 * <p>
 * Override {@link BlockEntity#getUpdateTag()} and {@link BlockEntity#getUpdatePacket()} to start the sync.
 * Usually they should return {@link #getCommonUpdateTag()} and {@link #createCommonUpdatePacket()} respectively.
 */
public interface LazyUpdateBlockEntity extends IForgeBlockEntity {
    /**
     * Check if the block entity should request a lazy update, usually by comparing the block state which should always be synced.
     */
    boolean shouldRequestLazyUpdate();

    /**
     * Send a packet to request the lazy update tag.
     */
    void sendLazyUpdateRequest();

    /**
     * Get the tag that will be sent only when requested.
     */
    CompoundTag getLazyUpdateTag();

    /**
     * Check if the tag is a lazy update tag, usually by checking if it contains a specific tag.
     */
    boolean isLazyUpdateTag(CompoundTag tag);

    void handleLazyUpdateTag(CompoundTag tag);

    /**
     * Get the tag that will be sent in every update.
     * Empty tag will not be sent.
     */
    CompoundTag getCommonUpdateTag();

    /**
     * Handle the common update tag.
     * <p>
     * Do NOT use update tag to sync empty item, because the empty tag will not be sent.
     * <p>
     * You can set the item to empty (remove the item) by checking the block state in this method (if you sure the common update tag will always be sent) or
     * {@link net.minecraftforge.common.extensions.IForgeBlock#onBlockStateChange(LevelReader, BlockPos, BlockState, BlockState) onBlockStateChange},
     * then remove the item if the block state says so and there will be no need to send the lazy update request.
     */
    void handleCommonUpdateTag(CompoundTag tag);

    default Packet<ClientGamePacketListener> createCommonUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create((BlockEntity) this);
    }

    @Override
    default void handleUpdateTag(CompoundTag compoundTag) {
        if (isLazyUpdateTag(compoundTag)) {
            handleLazyUpdateTag(compoundTag);
            return;
        }
        handleCommonUpdateTag(compoundTag);
        if (shouldRequestLazyUpdate()) {
            sendLazyUpdateRequest();
        }
    }

    default void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag compoundTag = pkt.getTag();
        if (compoundTag != null) {
            handleUpdateTag(compoundTag);
        }
    }
}
