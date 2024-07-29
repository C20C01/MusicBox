package io.github.c20c01.cc_mb.block.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
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
     * <p>
     * Can NOT return a empty tag
     */
    CompoundTag getCommonUpdateTag();

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
