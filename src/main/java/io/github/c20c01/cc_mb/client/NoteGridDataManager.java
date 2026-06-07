package io.github.c20c01.cc_mb.client;

import io.github.c20c01.cc_mb.data.NoteGridData;
import io.github.c20c01.cc_mb.network.NoteGridDataPacket;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntLinkedOpenHashSet;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.LinkedList;
import java.util.function.Consumer;

/**
 * Request and cache note grid data on the client side.
 * <p>
 * Use hash code to identify the data.
 */
public class NoteGridDataManager {
    private static final int REMOVABLE_SIZE = 16;
    private static NoteGridDataManager instance;
    private final Int2ObjectOpenHashMap<NoteGridData> cache = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectOpenHashMap<LinkedList<Consumer<NoteGridData>>> callbacks = new Int2ObjectOpenHashMap<>();
    private final IntLinkedOpenHashSet removable = new IntLinkedOpenHashSet(REMOVABLE_SIZE);

    private NoteGridDataManager() {
    }

    public static NoteGridDataManager getInstance() {
        if (instance == null) {
            instance = new NoteGridDataManager();
        }
        return instance;
    }

    public void handleReply(int hash, byte[] data) {
        // cache the data and call the callbacks
        NoteGridData noteGridData = NoteGridData.ofBytes(data);
        cache.put(hash, noteGridData);
        if (callbacks.containsKey(hash)) {
            callbacks.remove(hash).forEach(callback -> callback.accept(noteGridData));
        }
    }

    public void getNoteGridData(int hash, BlockPos blockPos, Consumer<NoteGridData> callback) {
        if (cache.containsKey(hash)) {
            // get from cache
            callback.accept(cache.get(hash));
            removable.remove(hash);
        } else {
            callback.accept(null);
            if (callbacks.containsKey(hash)) {
                // add to the callback list
                callbacks.get(hash).add(callback);
            } else {
                // new a callback list and send a request
                LinkedList<Consumer<NoteGridData>> callbacks = new LinkedList<>();
                callbacks.add(callback);
                this.callbacks.put(hash, callbacks);
                ClientPacketDistributor.sendToServer(new NoteGridDataPacket.Request(hash, blockPos));
            }
        }
    }

    /**
     * Mark the data with the hash code as removable.
     * It will be removed from the cache when the cache is full.
     */
    public void markRemovable(int hash) {
        if (removable.size() >= REMOVABLE_SIZE) {
            // remove the oldest one
            cache.remove(removable.removeFirstInt());
        }
        removable.add(hash);
    }
}
