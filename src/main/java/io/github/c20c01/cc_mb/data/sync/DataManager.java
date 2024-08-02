package io.github.c20c01.cc_mb.data.sync;

import com.mojang.logging.LogUtils;
import net.minecraftforge.network.NetworkEvent;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A data manager that manages the data that need to be synchronized from server to client.
 * Override your data's {@link Object#hashCode()} method before using this manager.
 * The data is stored as soft references.
 */
public abstract class DataManager<T> {
    // TODO Better cache cleaning？
    private final WeakValueMap<Integer, T> CACHE = new WeakValueMap<>();
    private final WeakValueMap<Integer, DataHolder<T>> REQUESTS = new WeakValueMap<>();

    /**
     * Send a request to the server to get the data which does not exist in the cache.
     */
    abstract void sendRequest(int hash);

    /**
     * Send the data to the client as a reply to the request.
     */
    abstract void sendReply(NetworkEvent.Context context, int hash, T data);

    public void put(Integer hash, T data) {
        CACHE.put(hash, data);
        LogUtils.getLogger().debug("{} with hash code: {} cached, cache size: {}", data.getClass().getSimpleName(), hash, CACHE.MAP.size());
    }

    /**
     * Get the data from the cache or request it from the server.
     * <p>
     * On the server side: You must {@link #put(Integer, Object) put} your data before getting it.
     */
    public DataHolder<T> get(int hash) {
        return CACHE.get(hash).map(data -> new DataHolder<>(hash, data)).orElseGet(() -> request(hash));
    }

    /**
     * Client side only.
     * <p>
     * If you try to get data before it is put, it will also be called on the server side which is not expected.
     */
    private DataHolder<T> request(int hash) {
        return REQUESTS.get(hash).orElseGet(() -> {
            DataHolder<T> emptyHolder = DataHolder.empty();
            REQUESTS.put(hash, emptyHolder);
            sendRequest(hash);
            return emptyHolder;
        });
    }

    /**
     * Server side only.
     */
    public void handleRequest(NetworkEvent.Context context, int hash) {
        CACHE.get(hash).ifPresentOrElse(data -> sendReply(context, hash, data), () -> LogUtils.getLogger().warn("Data with hash code: {} not found!", hash));
    }

    /**
     * Client side only.
     */
    public void handleReply(int hash, T data) {
        put(hash, data);
        REQUESTS.get(hash).ifPresent(dataHolder -> {
            REQUESTS.MAP.remove(hash);
            dataHolder.set(hash, data);
        });
    }

    /**
     * A map that stores the values as weak references.
     */
    private static class WeakValueMap<K, V> {
        final ReferenceQueue<V> QUEUE = new ReferenceQueue<>();
        final Map<K, WeakReference<V>> MAP = new HashMap<>();

        void put(K key, V value) {
            clean();
            MAP.put(key, new WeakReference<>(value, QUEUE));
        }

        Optional<V> get(K key) {
            clean();
            WeakReference<V> reference = MAP.get(key);
            if (reference == null) {
                return Optional.empty();
            }
            V value = reference.get();
            if (value == null) {
                MAP.remove(key);
            }
            return Optional.ofNullable(value);
        }

        void clean() {
            while (true) {
                WeakReference<?> reference = (WeakReference<?>) QUEUE.poll();
                if (reference == null) {
                    break;
                }
                MAP.values().remove(reference);
            }
        }
    }
}
