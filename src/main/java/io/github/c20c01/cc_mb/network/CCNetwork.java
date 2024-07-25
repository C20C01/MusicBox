package io.github.c20c01.cc_mb.network;


import io.github.c20c01.cc_mb.CCMain;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class CCNetwork {
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(CCMain.CHANNEL_ID, () -> CCMain.NETWORK_VERSION, CCMain.NETWORK_VERSION::equals, CCMain.NETWORK_VERSION::equals);

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        // Tell the server to punch the note grid at specific position.
        CHANNEL.messageBuilder(NoteGridPunchPacket.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(NoteGridPunchPacket::encode)
                .decoder(NoteGridPunchPacket::decode)
                .consumerMainThread(NoteGridPunchPacket::handleOnServer)
                .add();

        // Tell the server to update the sound shard with specific sound event name.
        CHANNEL.messageBuilder(SoundShardPacket.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SoundShardPacket::encode)
                .decoder(SoundShardPacket::decode)
                .consumerMainThread(SoundShardPacket::handleOnServer)
                .add();

        // Ask the server to send the note grid data.
        CHANNEL.messageBuilder(NoteGridRequestPacket.class, 2, NetworkDirection.PLAY_TO_SERVER)
                .encoder(NoteGridRequestPacket::encode)
                .decoder(NoteGridRequestPacket::decode)
                .consumerMainThread(NoteGridRequestPacket::handleOnServer)
                .add();

        // Send the note grid data to the client.
        CHANNEL.messageBuilder(NoteGridDataPacket.class, 3, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(NoteGridDataPacket::encode)
                .decoder(NoteGridDataPacket::decode)
                .consumerMainThread(NoteGridDataPacket::handleOnClient)
                .add();
    }
}