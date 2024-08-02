package io.github.c20c01.cc_mb.data.sync;

import javax.annotation.Nullable;
import java.util.Optional;

public class DataHolder<T> {
    @Nullable
    private Integer hashCode;
    @Nullable
    private T data;

    /**
     * A data holder that holds the data and its hash code.
     *
     * @param hashCode Used to synchronize data between server and client. Null if the data does not need to be synchronized.
     */
    public DataHolder(@Nullable Integer hashCode, @Nullable T data) {
        this.hashCode = hashCode;
        this.data = data;
    }

    public static <T> DataHolder<T> empty() {
        return new DataHolder<>(null, null);
    }

    public void set(int hashCode, T data) {
        this.hashCode = hashCode;
        this.data = data;
    }

    public Optional<T> get() {
        return Optional.ofNullable(data);
    }

    public Optional<Integer> getHashCode() {
        return Optional.ofNullable(hashCode);
    }

    public void clear() {
        hashCode = null;
        data = null;
    }
}
