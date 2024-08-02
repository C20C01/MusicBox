package io.github.c20c01.cc_mb.data.sync;

import io.github.c20c01.cc_mb.data.NoteGridData;
import io.github.c20c01.cc_mb.network.CCNetwork;
import io.github.c20c01.cc_mb.network.NoteGridDataPacket;
import net.minecraftforge.network.NetworkEvent;

public class NoteGridDataManager extends DataManager<NoteGridData> {
    public static final NoteGridDataManager INSTANCE = new NoteGridDataManager();

    private NoteGridDataManager() {
    }

    @Override
    void sendRequest(int hash) {
        CCNetwork.CHANNEL.sendToServer(new NoteGridDataPacket.ToServer(hash));
    }

    @Override
    void sendReply(NetworkEvent.Context context, int hash, NoteGridData data) {
        CCNetwork.CHANNEL.reply(new NoteGridDataPacket.ToClient(hash, data.toBytes()), context);
    }
}
