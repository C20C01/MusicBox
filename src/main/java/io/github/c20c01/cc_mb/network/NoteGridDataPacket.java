package io.github.c20c01.cc_mb.network;

import io.github.c20c01.cc_mb.data.ClientNoteGridManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public record NoteGridDataPacket(int noteGridId, @Nullable byte[] data) {
    public static NoteGridDataPacket decode(FriendlyByteBuf friendlyByteBuf) {
        int noteGridId = friendlyByteBuf.readVarInt();
        boolean isLatest = friendlyByteBuf.readBoolean();
        byte[] data = isLatest ? null : friendlyByteBuf.readByteArray();
        return new NoteGridDataPacket(noteGridId, data);
    }

    public void encode(FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeVarInt(noteGridId);
        friendlyByteBuf.writeBoolean(data == null);
        if (data != null) {
            friendlyByteBuf.writeByteArray(data);
        }
    }

    public void handleOnClient(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ClientNoteGridManager.handleResponse(noteGridId, data));
        context.setPacketHandled(true);
    }
}
