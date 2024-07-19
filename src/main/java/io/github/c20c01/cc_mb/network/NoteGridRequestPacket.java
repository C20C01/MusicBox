package io.github.c20c01.cc_mb.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record NoteGridRequestPacket() {
    public static NoteGridPacket decode(FriendlyByteBuf friendlyByteBuf) {
        return new NoteGridPacket(friendlyByteBuf.readVarInt(), friendlyByteBuf.readByte(), friendlyByteBuf.readByte(), friendlyByteBuf.readByte());
    }

    public void encode(FriendlyByteBuf friendlyByteBuf) {

    }

    public void handleOnServer(Supplier<NetworkEvent.Context> contextSupplier) {

    }
}
