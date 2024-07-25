package io.github.c20c01.cc_mb.network;

import io.github.c20c01.cc_mb.data.ServerNoteGridManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record NoteGridPunchPacket(int noteGridId, byte page, byte beat, byte note) {
    public static NoteGridPunchPacket decode(FriendlyByteBuf friendlyByteBuf) {
        return new NoteGridPunchPacket(friendlyByteBuf.readVarInt(), friendlyByteBuf.readByte(), friendlyByteBuf.readByte(), friendlyByteBuf.readByte());
    }

    public void encode(FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeVarInt(noteGridId);
        friendlyByteBuf.writeByte(page);
        friendlyByteBuf.writeByte(beat);
        friendlyByteBuf.writeByte(note);
    }

    public void handleOnServer(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ServerNoteGridManager.handlePunchGrid(context.getSender(), noteGridId, page, beat, note));
        context.setPacketHandled(true);
    }
}