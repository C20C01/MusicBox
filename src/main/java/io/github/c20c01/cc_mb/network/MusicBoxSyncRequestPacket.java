package io.github.c20c01.cc_mb.network;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.block.entity.MusicBoxBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Packet sent from client to server to sync the content of a {@link MusicBoxBlockEntity} at a specific position.
 */
public record MusicBoxSyncRequestPacket(BlockPos pos) {
    public static MusicBoxSyncRequestPacket decode(FriendlyByteBuf friendlyByteBuf) {
        return new MusicBoxSyncRequestPacket(friendlyByteBuf.readBlockPos());
    }

    private static void syncData(@Nullable ServerPlayer player, BlockPos pos) {
        if (player != null) {
            // When the player sends the packet, the music box is already loaded.
            player.level().getBlockEntity(pos, CCMain.MUSIC_BOX_BLOCK_ENTITY.get()).ifPresent(blockEntity -> {
                ClientboundBlockEntityDataPacket packet = ClientboundBlockEntityDataPacket.create(blockEntity, b -> blockEntity.getLazyUpdateTag());
                player.connection.send(packet);
            });
        }
    }

    public void encode(FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeBlockPos(pos);
    }

    public void handleOnServer(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> syncData(context.getSender(), pos));
        context.setPacketHandled(true);
    }
}
