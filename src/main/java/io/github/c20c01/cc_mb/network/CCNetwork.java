package io.github.c20c01.cc_mb.network;


import io.github.c20c01.cc_mb.CCMain;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class CCNetwork {
    public static final SimpleChannel CHANNEL_GRID_TO_S = NetworkRegistry.newSimpleChannel(CCMain.CHANNEL_GRID_TO_S, () -> CCMain.NETWORK_VERSION, CCMain.NETWORK_VERSION::equals, CCMain.NETWORK_VERSION::equals);
    public static final SimpleChannel CHANNEL_SOUND_SHARD_TO_S = NetworkRegistry.newSimpleChannel(CCMain.CHANNEL_SOUND_SHARD_TO_S, () -> CCMain.NETWORK_VERSION, CCMain.NETWORK_VERSION::equals, CCMain.NETWORK_VERSION::equals);

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        CHANNEL_GRID_TO_S.registerMessage(0, NoteGridPacket.class, NoteGridPacket::encode, NoteGridPacket::decode, NoteGridPacket::handleOnServer, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL_SOUND_SHARD_TO_S.registerMessage(0, SoundShardPacket.class, SoundShardPacket::encode, SoundShardPacket::decode, SoundShardPacket::handleOnServer, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
}