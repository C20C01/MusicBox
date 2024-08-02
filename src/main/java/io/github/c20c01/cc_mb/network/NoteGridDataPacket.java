package io.github.c20c01.cc_mb.network;

import io.github.c20c01.cc_mb.data.NoteGridData;
import io.github.c20c01.cc_mb.data.sync.NoteGridDataManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class NoteGridDataPacket {
    public record ToServer(int hash) {
        public static NoteGridDataPacket.ToServer decode(FriendlyByteBuf friendlyByteBuf) {
            return new NoteGridDataPacket.ToServer(friendlyByteBuf.readInt());
        }

        public void encode(FriendlyByteBuf friendlyByteBuf) {
            friendlyByteBuf.writeInt(hash);
        }

        public void handleOnServer(Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> NoteGridDataManager.INSTANCE.handleRequest(context, hash));
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
            context.enqueueWork(() -> NoteGridDataManager.INSTANCE.handleReply(hash, NoteGridData.ofBytes(data)));
            context.setPacketHandled(true);
        }
    }
}
