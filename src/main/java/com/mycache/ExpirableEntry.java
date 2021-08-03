package com.mycache;

import javax.cache.Cache;
import java.io.Serializable;

import static java.util.Objects.requireNonNull;

public class ExpirableEntry<K, V> implements Cache.Entry<K, V>, Serializable {

    private final K key;

    private V value;

    private long timestamp;

    public ExpirableEntry(K key, V value) throws NullPointerException {
        requireKeyNotNull(key);
        this.key = key;
        this.setValue(value);
        this.timestamp = Long.MAX_VALUE; // default
    }

    public static <K> void requireKeyNotNull(K key) {
        requireNonNull(key, "The key must not be null.");
    }


    public static <V> void requireValueNotNull(V value) {
        requireNonNull(value, "The value must not be null.");
    }


    public void setValue(V value) {
        requireKeyNotNull(value);
        this.value = value;
    }

    @Override
    public K getKey() {
        return key;
    }

    @Override
    public V getValue() {
        return value;
    }

    @Override
    public <T> T unwrap(Class<T> clazz) {
        return null;
    }
}
