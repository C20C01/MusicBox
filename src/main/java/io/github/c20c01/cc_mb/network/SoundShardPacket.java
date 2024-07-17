package io.github.c20c01.cc_mb.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SoundShardPacket(int slot, String sound) {
    public static SoundShardPacket decode(FriendlyByteBuf friendlyByteBuf) {
        return new SoundShardPacket(friendlyByteBuf.readVarInt(), friendlyByteBuf.readUtf());
    }

    public void encode(FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeVarInt(slot);
        friendlyByteBuf.writeUtf(sound);
    }

    public void handleOnServer(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            context.enqueueWork(() -> UpdateSoundShard.onServer(player, slot, sound));
        }
        context.setPacketHandled(true);
    }
}
