package com.mycache;

import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.configuration.Configuration;
import java.util.HashMap;

public class InMemoryCache<K, V> extends AbstractCache<K, V> {

    private CacheManager cacheManager;

    protected InMemoryCache(CacheManager cacheManager, String cacheName, Configuration<K, V> configuration) {
        super(cacheManager, cacheName, configuration);
        this.cacheManager = cacheManager;
    }

    HashMap<String, Entry<K, V>> hashMap = new HashMap<>();

    @Override
    protected void putEntry(Entry<K, V> entry) throws CacheException, ClassCastException {
        hashMap.put((String) entry.getKey(), entry);
        System.out.println("to " + cacheManager.getCacheNames() + " add key= " + entry.getKey() + " value = " + entry.getValue());
    }

    @Override
    protected ExpirableEntry<K, V> getEntry(K key) throws CacheException, ClassCastException {
        return (ExpirableEntry<K, V>) hashMap.get(key);
    }

    @Override
    protected boolean removeEntry(K key) {
        Entry<K, V> removed = hashMap.remove(key);

        System.out.println("from " + cacheManager.getCacheNames() + " remove key= " + removed.getKey() + " value = " + removed.getValue());
        return removed != null;
    }
}
