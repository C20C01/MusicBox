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
    private static final HashMap<Integer, NoteGridDataHolder> HOLDER_MAP = new HashMap<>();

    /**
     * Get the note grid data with the given id from the client's cache.
     * Also send a request to the server to get the latest data.<p>
     * See {@link ServerNoteGridManager} for the server side implementation.
     */
    @Nullable
    public static NoteGridData getNoteGridData(int noteGridId, @Nullable Consumer<NoteGridData> updater) {
        NoteGridDataHolder holder = HOLDER_MAP.computeIfAbsent(noteGridId, k -> new NoteGridDataHolder());
        holder.updater = updater;
        CCNetwork.CHANNEL.sendToServer(new NoteGridRequestPacket(noteGridId));
        return holder.noteGridData;
    }

    /**
     * Handle the response from the server.
     *
     * @param data null if the data is the latest
     */
    public static void handleResponse(int noteGridId, @Nullable byte[] data) {
        if (data == null) {
            if (HOLDER_MAP.containsKey(noteGridId)) {
                HOLDER_MAP.get(noteGridId).updater = null;
            }
        } else {
            NoteGridDataHolder holder = HOLDER_MAP.computeIfAbsent(noteGridId, k -> new NoteGridDataHolder());
            holder.noteGridData = NoteGridData.ofBytes(data);
            holder.update();
        }
    }

    @SubscribeEvent
    public static void OnPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        HOLDER_MAP.clear();
    }

    /**
     * {@code noteGridData} - Stores the holes of the note grid.<p>
     * {@code noteGridIdeaData} - Made in player's mind, not saved in the server.
     * It can be edited and played in player's mind, which guides the player to punch the holes on the real note grid.<p>
     * {@code updater} - The consumer to update the note grid data in note grid player.
     */
    private static class NoteGridDataHolder {
        NoteGridData noteGridData;
        NoteGridData noteGridIdeaData;
        Consumer<NoteGridData> updater;

        public void update() {
            if (updater != null) {
                updater.accept(noteGridData);
                updater = null;
            }
        }
    }
}
