package io.github.c20c01.cc_mb.network;

import io.github.c20c01.cc_mb.data.ServerNoteGridManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record NoteGridRequestPacket(int noteGridId) {
    public static NoteGridRequestPacket decode(FriendlyByteBuf friendlyByteBuf) {
        return new NoteGridRequestPacket(friendlyByteBuf.readVarInt());
    }

    public void encode(FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeVarInt(noteGridId);
    }

    public void handleOnServer(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ServerNoteGridManager.handleNoteGridRequest(noteGridId, context.getSender()));
        context.setPacketHandled(true);
    }
}
