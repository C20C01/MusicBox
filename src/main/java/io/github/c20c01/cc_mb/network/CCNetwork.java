package io.github.c20c01.cc_mb.network;


import io.github.c20c01.cc_mb.CCMain;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

@Mod.EventBusSubscriber(modid = CCMain.ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CCNetwork {
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(CCMain.CHANNEL_ID, () -> CCMain.NETWORK_VERSION, CCMain.NETWORK_VERSION::equals, CCMain.NETWORK_VERSION::equals);

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        int id = -1;// use ++id to get the next id

        // Tell the server to update the sound shard with specific sound event name.
        CHANNEL.messageBuilder(SoundShardUpdatePacket.class, ++id, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SoundShardUpdatePacket::encode)
                .decoder(SoundShardUpdatePacket::decode)
                .consumerMainThread(SoundShardUpdatePacket::handleOnServer)
                .add();

        // Ask the server to send the note grid data with specific hash code.
        CHANNEL.messageBuilder(NoteGridDataPacket.ToServer.class, ++id, NetworkDirection.PLAY_TO_SERVER)
                .encoder(NoteGridDataPacket.ToServer::encode)
                .decoder(NoteGridDataPacket.ToServer::decode)
                .consumerMainThread(NoteGridDataPacket.ToServer::handleOnServer)
                .add();

        // Send the note grid data to the client with specific hash code.
        CHANNEL.messageBuilder(NoteGridDataPacket.ToClient.class, ++id, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(NoteGridDataPacket.ToClient::encode)
                .decoder(NoteGridDataPacket.ToClient::decode)
                .consumerMainThread(NoteGridDataPacket.ToClient::handleOnClient)
                .add();
    }
}