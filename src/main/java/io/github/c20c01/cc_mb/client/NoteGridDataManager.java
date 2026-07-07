package io.github.c20c01.cc_mb.client;

import io.github.c20c01.cc_mb.data.NoteGridData;
import io.github.c20c01.cc_mb.network.NoteGridDataPacket;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.function.Consumer;

/**
 * Request and cache note grid data on the client side.
 * <p>
 * Use hash code to identify the data.
 * <p>
 * No cleaning inside but the cache size is limited.
 */
public class NoteGridDataManager {
    private static final int MAX_CACHE_SIZE = 64;
    private static NoteGridDataManager instance;
    private final Int2ObjectLinkedOpenHashMap<NoteGridData> cache;
    private final Int2ObjectOpenHashMap<Consumer<NoteGridData>> callbacks;

    private NoteGridDataManager() {
        cache = new Int2ObjectLinkedOpenHashMap<>(MAX_CACHE_SIZE);
        callbacks = new Int2ObjectOpenHashMap<>();
    }

    public static NoteGridDataManager getInstance() {
        if (instance == null) {
            instance = new NoteGridDataManager();
        }
        return instance;
    }

    public void handleReply(int hash, byte[] data) {
        if (cache.size() >= MAX_CACHE_SIZE) {
            cache.removeFirst();
        }

        NoteGridData noteGridData = NoteGridData.ofBytes(data);
        cache.put(hash, noteGridData);

        if (callbacks.containsKey(hash)) {
            callbacks.remove(hash).accept(noteGridData);
        }
    }

    /**
     * Request the note grid data with the hash code, there are two cases:
     * <li>If the data is in the cache, return it immediately.</li>
     * <li>If the data is not in the cache, return null first, then wait for the server reply and return data again.</li>
     *
     * @param callback the callback to receive the data, may be called with null if the data is not in the cache.
     */
    public void requestData(int hash, BlockPos blockPos, Consumer<NoteGridData> callback) {
        NoteGridData cached = cache.getAndMoveToLast(hash);
        if (cached != null) {
            callback.accept(cached);
            return;
        }

        callback.accept(null);
        Consumer<NoteGridData> existingCallback = callbacks.get(hash);
        if (existingCallback != null) {
            callbacks.put(hash, existingCallback.andThen(callback));
            return;
        }

        callbacks.put(hash, callback);
        ClientPacketDistributor.sendToServer(new NoteGridDataPacket.Request(hash, blockPos));
    }
}
