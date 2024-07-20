package io.github.c20c01.cc_mb.data;

import com.mojang.logging.LogUtils;
import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.network.CCNetwork;
import io.github.c20c01.cc_mb.network.NoteGridDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;

@Mod.EventBusSubscriber(modid = CCMain.ID)
public class ServerNoteGridManager {
    private static final HashMap<Integer, HashSet<ServerPlayer>> LATEST_DATA_PLAYER_MAP = new HashMap<>();// players who have the latest data

    /**
     * Send the note grid data when the client requests it and its data is not the latest.
     */
    public static void handleNoteGridRequest(int noteGridId, @Nullable ServerPlayer player) {
        if (player != null) {
            LATEST_DATA_PLAYER_MAP.putIfAbsent(noteGridId, new HashSet<>());
            byte[] responseData = LATEST_DATA_PLAYER_MAP.get(noteGridId).add(player) ? getNoteGridData(noteGridId, player) : null;
            CCNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new NoteGridDataPacket(noteGridId, responseData));
        }
    }

    @Nullable
    private static byte[] getNoteGridData(int noteGridId, ServerPlayer player) {
        NoteGridData$ data = NoteGridData$.ofId(player.server, noteGridId);
        if (data == null) {
            LogUtils.getLogger().warn("{} requested note grid data with id {}, but it does not exist", player.getName().getString(), noteGridId);
            return null;
        }
        return data.toBytes();
    }

    public static void makeDirty(int noteGridId) {
        if (LATEST_DATA_PLAYER_MAP.containsKey(noteGridId)) {
            LATEST_DATA_PLAYER_MAP.get(noteGridId).clear();
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer) {
            LATEST_DATA_PLAYER_MAP.values().forEach(players -> players.remove(player));
        }
    }
}
