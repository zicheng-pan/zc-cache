package com.mycache;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.configuration.*;
import javax.cache.expiry.EternalExpiryPolicy;
import javax.cache.expiry.ExpiryPolicy;
import javax.cache.integration.CompletionListener;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.EntryProcessorResult;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public abstract class AbstractCache<K, V> implements Cache<K, V> {


    private final CacheManager cacheManager;
    private final String cacheName;
    private final MutableConfiguration<K, V> configuration;
    private final ExpiryPolicy expiryPolicy;

    protected AbstractCache(CacheManager cacheManager, String cacheName, Configuration<K, V> configuration) {
        this.cacheManager = cacheManager;
        this.cacheName = cacheName;
        this.configuration = mutableConfiguration(configuration);
        this.expiryPolicy = resolveExpiryPolicy(this.getConfiguration());
        //TODO set a fallback stragey when  cannout load cache
        //        this.defaultFallbackStorage = new CompositeFallbackStorage(getClassLoader());
//        this.cacheLoader = resolveCacheLoader(getConfiguration(), getClassLoader());
//        this.cacheWriter = resolveCacheWriter(getConfiguration(), getClassLoader());
//        this.entryEventPublisher = new CacheEntryEventPublisher();
//        this.cacheStatistics = resolveCacheStatistic();
//        this.executor = ForkJoinPool.commonPool();
//        registerCacheEntryListenersFromConfiguration();
//        registerMBeansIfRequired(this, cacheStatistics);
    }


    private ExpiryPolicy resolveExpiryPolicy(CompleteConfiguration<?, ?> configuration) {
        Factory<ExpiryPolicy> expiryPolicyFactory = configuration.getExpiryPolicyFactory();
        if (expiryPolicyFactory == null) {
            expiryPolicyFactory = EternalExpiryPolicy::new;
        }
        return expiryPolicyFactory.create();
    }

    /**
     * As an instance of {@link MutableConfiguration}
     *
     * @param configuration {@link Configuration}
     * @param <K>           the type of key
     * @param <V>           the type of value
     * @return non-null
     * @see MutableConfiguration
     */
    public static <K, V> MutableConfiguration<K, V> mutableConfiguration(Configuration<K, V> configuration) {
        MutableConfiguration mutableConfiguration = null;
        if (configuration instanceof MutableConfiguration) {
            mutableConfiguration = (MutableConfiguration) configuration;
        } else if (configuration instanceof CompleteConfiguration) {
            CompleteConfiguration config = (CompleteConfiguration) configuration;
            mutableConfiguration = new MutableConfiguration<>(config);
        } else {
            mutableConfiguration = new MutableConfiguration<K, V>()
                    .setTypes(configuration.getKeyType(), configuration.getValueType())
                    .setStoreByValue(configuration.isStoreByValue());
        }
        return mutableConfiguration;
    }

    @Override
    public V get(K key) {
        //TODO 完善 isReadThrough 和 isWriteThrough 功能
        assertNotClosed();
        requireKeyNotNull(key);
        return getEntry(key).getValue();
    }

    @Override
    public Map<K, V> getAll(Set<? extends K> keys) {
        return null;
    }

    @Override
    public boolean containsKey(K key) {
        return false;
    }

    @Override
    public void loadAll(Set<? extends K> keys, boolean replaceExistingValues, CompletionListener completionListener) {

    }

    @Override
    public void put(K key, V value) {
        ExpirableEntry expirableEntry = new ExpirableEntry(key, value);
        putEntry(expirableEntry);
    }

    protected abstract void putEntry(Entry<K, V> entry) throws CacheException, ClassCastException;

    protected abstract ExpirableEntry<K, V> getEntry(K key) throws CacheException, ClassCastException;

    protected abstract boolean removeEntry(K key);

    @Override
    public V getAndPut(K key, V value) {
        return null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {

    }

    @Override
    public boolean putIfAbsent(K key, V value) {
        return false;
    }

    @Override
    public boolean remove(K key) {
        assertNotClosed();
        requireKeyNotNull(key);
        return removeEntry(key);
    }

    @Override
    public boolean remove(K key, V oldValue) {
        return false;
    }

    @Override
    public V getAndRemove(K key) {
        return null;
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return false;
    }

    @Override
    public boolean replace(K key, V value) {
        return false;
    }

    @Override
    public V getAndReplace(K key, V value) {
        return null;
    }

    @Override
    public void removeAll(Set<? extends K> keys) {

    }

    @Override
    public void removeAll() {

    }

    @Override
    public void clear() {

    }


    // Operations of CompleteConfiguration

    protected final CompleteConfiguration<K, V> getConfiguration() {
        return this.configuration;
    }

    @Override
    public <C extends Configuration<K, V>> C getConfiguration(Class<C> clazz) {
        if (!Configuration.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException("The class must be inherited of " + Configuration.class.getName());
        }
        return (C) this.configuration;
    }

    @Override
    public <T> T invoke(K key, EntryProcessor<K, V, T> entryProcessor, Object... arguments) throws EntryProcessorException {
        return null;
    }

    @Override
    public <T> Map<K, EntryProcessorResult<T>> invokeAll(Set<? extends K> keys, EntryProcessor<K, V, T> entryProcessor, Object... arguments) {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public CacheManager getCacheManager() {
        return null;
    }

    @Override
    public void close() {

    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> clazz) {
        return null;
    }

    @Override
    public void registerCacheEntryListener(CacheEntryListenerConfiguration<K, V> cacheEntryListenerConfiguration) {

    }

    @Override
    public void deregisterCacheEntryListener(CacheEntryListenerConfiguration<K, V> cacheEntryListenerConfiguration) {

    }

    @Override
    public Iterator<Entry<K, V>> iterator() {
        return null;
    }

    private void assertNotClosed() {
        if (isClosed()) {
            throw new IllegalStateException("Current cache has been closed! No operation should be executed.");
        }
    }

    public static <K> void requireKeyNotNull(K key) {
        requireNonNull(key, "The key must not be null.");
    }
}
