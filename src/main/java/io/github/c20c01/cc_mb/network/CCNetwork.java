package io.github.c20c01.cc_mb.network;


import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.client.gui.PerforationTableMenu;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;
import java.util.function.Supplier;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class CCNetwork {
    public static final SimpleChannel CHANNEL_GRID_TO_S = NetworkRegistry.newSimpleChannel(CCMain.CHANNEL_GRID_TO_S, () -> CCMain.NETWORK_VERSION, CCMain.NETWORK_VERSION::equals, CCMain.NETWORK_VERSION::equals);

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        CHANNEL_GRID_TO_S.registerMessage(0, NoteGridPacket.class, NoteGridPacket::encode, NoteGridPacket::decode, NoteGridPacket::handleOnServer, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

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
}