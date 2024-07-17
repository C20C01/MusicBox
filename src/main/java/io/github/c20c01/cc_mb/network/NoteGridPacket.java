package io.github.c20c01.cc_mb.network;

import io.github.c20c01.cc_mb.client.gui.PerforationTableMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record NoteGridPacket(int containerId, byte page, byte beat, byte note) {
    public static NoteGridPacket decode(FriendlyByteBuf friendlyByteBuf) {
        return new NoteGridPacket(friendlyByteBuf.readVarInt(), friendlyByteBuf.readByte(), friendlyByteBuf.readByte(), friendlyByteBuf.readByte());
    }

    public void encode(FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeVarInt(containerId);
        friendlyByteBuf.writeByte(page);
        friendlyByteBuf.writeByte(beat);
        friendlyByteBuf.writeByte(note);
    }

    public void handleOnServer(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player == null) {
            context.setPacketHandled(true);
            return;
        }

        AbstractContainerMenu containerMenu = player.containerMenu;
        if (containerMenu.containerId == containerId && containerMenu instanceof PerforationTableMenu menu) {
            if (menu.stillValid(player) && !player.isSpectator()) {
                context.enqueueWork(() -> menu.punchGridOnServer(player, page, beat, note));
            }
        }

        context.setPacketHandled(true);
    }
}