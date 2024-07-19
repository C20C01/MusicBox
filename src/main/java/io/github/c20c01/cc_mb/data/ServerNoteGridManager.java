package io.github.c20c01.cc_mb.data;

import com.mojang.logging.LogUtils;
import io.github.c20c01.cc_mb.CCMain;
import io.github.c20c01.cc_mb.network.CCNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;

@OnlyIn(Dist.DEDICATED_SERVER)
@Mod.EventBusSubscriber(value = Dist.DEDICATED_SERVER, modid = CCMain.ID)
public class ServerNoteGridManager {
    private static final HashMap<Integer, HashSet<Player>> ALREADY_SENT_MAP = new HashMap<>();

    public static void handleNoteGridRequest(int noteGridId, Player player) {
        if (ALREADY_SENT_MAP.containsKey(noteGridId)) {
            if (ALREADY_SENT_MAP.get(noteGridId).add(player)) {
                //TODO Send note grid data to player
            }
        } else {
            HashSet<Player> players = new HashSet<>();
            players.add(player);
            ALREADY_SENT_MAP.put(noteGridId, players);
            //TODO Send note grid data to player
        }
    }

    private static void sendNoteGridData(int noteGridId, ServerPlayer player) {
        NoteGridData$ data = NoteGridData$.ofId(player.server, noteGridId);
        if (data == null) {
            LogUtils.getLogger().warn("{} requested note grid data with id {} but it does not exist", player.getName(), noteGridId);
        }

    }
}
