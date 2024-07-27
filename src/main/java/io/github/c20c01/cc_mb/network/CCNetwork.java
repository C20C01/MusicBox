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
        int id = -1;// use ++id to get the next id

        // Tell the server to update the sound shard with specific sound event name.
        CHANNEL.messageBuilder(SoundShardPacket.class, ++id, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SoundShardPacket::encode)
                .decoder(SoundShardPacket::decode)
                .consumerMainThread(SoundShardPacket::handleOnServer)
                .add();

        // Ask the server to sync the music box's content at specific position.
        CHANNEL.messageBuilder(MusicBoxSyncRequestPacket.class, ++id, NetworkDirection.PLAY_TO_SERVER)
                .encoder(MusicBoxSyncRequestPacket::encode)
                .decoder(MusicBoxSyncRequestPacket::decode)
                .consumerMainThread(MusicBoxSyncRequestPacket::handleOnServer)
                .add();
    }
}