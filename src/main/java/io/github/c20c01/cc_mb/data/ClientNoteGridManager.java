package io.github.c20c01.cc_mb.data;

import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.network.CCNetwork;
import io.github.c20c01.cc_mb.network.NoteGridRequestPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = CCMain.ID)
public class ClientNoteGridManager {
    private static final HashMap<Integer, NoteGridData$> NOTE_GRID_DATA_MAP = new HashMap<>();
    private static final HashMap<Integer, Consumer<NoteGridData$>> UPDATER_MAP = new HashMap<>();

    /**
     * Get the note grid data with the given id from the client's cache.
     * Also send a request to the server to get the latest data.<p>
     * See {@link ServerNoteGridManager} for the server side implementation.
     */
    @Nullable
    public static NoteGridData$ getNoteGridData(int noteGridId, Consumer<NoteGridData$> updater) {
        UPDATER_MAP.put(noteGridId, updater);
        CCNetwork.CHANNEL.sendToServer(new NoteGridRequestPacket(noteGridId));
        return NOTE_GRID_DATA_MAP.get(noteGridId);
    }

    /**
     * Handle the response from the server.
     *
     * @param data null if the data is the latest
     */
    public static void handleResponse(int noteGridId, @Nullable byte[] data) {
        var updater = UPDATER_MAP.remove(noteGridId);
        if (data != null) {
            NoteGridData$ noteGridData = NoteGridData$.ofBytes(data);
            NOTE_GRID_DATA_MAP.put(noteGridId, noteGridData);
            if (updater != null) {
                updater.accept(noteGridData);
            }
        }
    }

    @SubscribeEvent
    public static void OnPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        NOTE_GRID_DATA_MAP.clear();
        UPDATER_MAP.clear();
    }
}
