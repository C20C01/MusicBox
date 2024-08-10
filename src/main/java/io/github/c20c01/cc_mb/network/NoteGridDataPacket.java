package io.github.c20c01.cc_mb.network;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.block.entity.MusicBoxBlockEntity;
import io.github.c20c01.cc_mb.client.NoteGridDataManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

public class NoteGridDataPacket {
    public record ToServer(int hash, BlockPos blockPos) {
        public static NoteGridDataPacket.ToServer decode(FriendlyByteBuf friendlyByteBuf) {
            return new NoteGridDataPacket.ToServer(friendlyByteBuf.readInt(), friendlyByteBuf.readBlockPos());
        }

        private static void tryToReply(NetworkEvent.Context context, int hash, BlockPos blockPos) {
            ServerPlayer player = context.getSender();
            if (player != null) {
                player.serverLevel().getBlockEntity(blockPos, CCMain.MUSIC_BOX_BLOCK_ENTITY.get())
                        .flatMap(MusicBoxBlockEntity::getPlayerData)
                        .ifPresent(noteGridData -> CCNetwork.CHANNEL.reply(new ToClient(hash, noteGridData.toBytes()), context));
            }
        }

        public void encode(FriendlyByteBuf friendlyByteBuf) {
            friendlyByteBuf.writeInt(hash);
            friendlyByteBuf.writeBlockPos(blockPos);
        }

        public void handleOnServer(Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> tryToReply(context, hash, blockPos));
            context.setPacketHandled(true);
        }
    }

    public record ToClient(int hash, byte[] data) {
        public static NoteGridDataPacket.ToClient decode(FriendlyByteBuf friendlyByteBuf) {
            return new NoteGridDataPacket.ToClient(friendlyByteBuf.readInt(), friendlyByteBuf.readByteArray());
        }

        public void encode(FriendlyByteBuf friendlyByteBuf) {
            friendlyByteBuf.writeInt(hash);
            friendlyByteBuf.writeByteArray(data);
        }

        public void handleOnClient(Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> NoteGridDataManager.getInstance().handleReply(hash, data));
            context.setPacketHandled(true);
        }
    }
}
