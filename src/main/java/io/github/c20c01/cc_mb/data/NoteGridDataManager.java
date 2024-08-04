package io.github.c20c01.cc_mb.data;

import io.github.c20c01.cc_mb.network.CCNetwork;
import io.github.c20c01.cc_mb.network.NoteGridDataPacket;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntLinkedOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Consumer;

/**
 * Request and cache note grid data on the client side.
 */
@OnlyIn(Dist.CLIENT)
public class NoteGridDataManager {
    private static final int CACHE_SIZE = 16;// max size of REMOVABLE
    private static NoteGridDataManager instance;
    private final Int2ObjectOpenHashMap<NoteGridData> CACHE = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectOpenHashMap<Consumer<NoteGridData>> CALLBACKS = new Int2ObjectOpenHashMap<>();
    private final IntLinkedOpenHashSet REMOVABLE = new IntLinkedOpenHashSet(CACHE_SIZE);

    private NoteGridDataManager() {
    }

    public static NoteGridDataManager getInstance() {
        if (instance == null) {
            instance = new NoteGridDataManager();
        }
        return instance;
    }

    public void handleReply(int hash, byte[] data) {
        // get from server
        NoteGridData noteGridData = NoteGridData.ofBytes(data);
        CACHE.put(hash, noteGridData);
        if (CALLBACKS.containsKey(hash)) {
            CALLBACKS.remove(hash).accept(noteGridData);
        }
    }

    public void getNoteGridData(int hash, BlockPos blockPos, Consumer<NoteGridData> callback) {
        if (CACHE.containsKey(hash)) {
            // get from cache
            callback.accept(CACHE.get(hash));
            REMOVABLE.remove(hash);
        } else {
            callback.accept(null);
            CCNetwork.CHANNEL.sendToServer(new NoteGridDataPacket.ToServer(hash, blockPos));
            CALLBACKS.put(hash, callback);
        }
    }

    /**
     * Mark the data with the hash code as removable.
     * It will be removed from the cache when the cache is full.
     */
    public void markRemovable(int hash) {
        if (REMOVABLE.size() >= CACHE_SIZE) {
            // remove the oldest one
            CACHE.remove(REMOVABLE.removeFirstInt());
        }
        REMOVABLE.add(hash);
    }
}
