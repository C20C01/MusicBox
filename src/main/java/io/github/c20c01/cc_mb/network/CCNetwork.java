package io.github.c20c01.cc_mb.network;


import io.github.c20c01.cc_mb.CCMain;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = CCMain.ID)
public class CCNetwork {

    @SubscribeEvent
    public static void registerPayload(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(CCMain.NETWORK_VERSION);

        // Request the note grid data from the server.
        registrar.playToServer(NoteGridDataPacket.Request.TYPE, NoteGridDataPacket.Request.STREAM_CODEC, NoteGridDataPacket.Request::handle);

        // Reply the note grid data to the client.
        registrar.playToClient(NoteGridDataPacket.Reply.TYPE, NoteGridDataPacket.Reply.STREAM_CODEC, NoteGridDataPacket.Reply::handle);

        // Tell the server to update the sound shard with specific sound event name.
        registrar.playToServer(SoundShardUpdatePacket.TYPE, SoundShardUpdatePacket.STREAM_CODEC, SoundShardUpdatePacket::handle);
    }
}