package com.mycache;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.configuration.Configuration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;
import java.net.URI;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public abstract class AbstractCacheManager implements CacheManager {

    private class CacheIdentifier {
        KeyValueTypePair keyValueTypePair;
        Cache cache;
    }

    private final CachingProvider cachingProvider;
    private final URI uri;
    private final Properties properties;
    private final ClassLoader classLoader;
    private boolean isClosed = false;

    // 用cache对象的key value类型作为唯一标识 标识这个cache
    private ConcurrentMap<String, CacheIdentifier> cacheRepository = new ConcurrentHashMap<>();


    //TODO cache 序列化工具 因为缓存不会所有的都存字符串
    //    private final Object serializers;
    //    private final Object deserializers;

    public AbstractCacheManager(CachingProvider cachingProvider, URI uri, ClassLoader classLoader, Properties properties) {
        this.cachingProvider = cachingProvider;
        this.uri = uri == null ? cachingProvider.getDefaultURI() : uri;
        this.properties = properties == null ? cachingProvider.getDefaultProperties() : properties;
        this.classLoader = classLoader == null ? cachingProvider.getDefaultClassLoader() : classLoader;
    }

    @Override
    public CachingProvider getCachingProvider() {
        return cachingProvider;
    }

    @Override
    public URI getURI() {
        return uri;
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public Properties getProperties() {
        return properties;
    }

    /**
     * Creates a named {@link Cache} at runtime.
     * <p>
     * If a {@link Cache} with the specified name is known to the {@link
     * CacheManager}, a CacheException is thrown.
     * <p>
     * If a {@link Cache} with the specified name is unknown the {@link
     * CacheManager}, one is created according to the provided {@link Configuration}
     * after which it becomes managed by the {@link CacheManager}.
     * <p>
     * Prior to a {@link Cache} being created, the provided {@link Configuration}s is
     * validated within the context of the {@link CacheManager} properties and
     * implementation.
     * <p>
     * Implementers should be aware that the {@link Configuration} may be used to
     * configure other {@link Cache}s.
     * <p>
     * There's no requirement on the part of a developer to call this method for
     * each {@link Cache} an application may use.  Implementations may support
     * the use of declarative mechanisms to pre-configure {@link Cache}s, thus
     * removing the requirement to configure them in an application.  In such
     * circumstances a developer may simply call either the
     * {@link #getCache(String)} or {@link #getCache(String, Class, Class)}
     * methods to acquire a previously established or pre-configured {@link Cache}.
     *
     * @param <K>           the type of key
     * @param <V>           the type of value
     * @param <C>           the type of the Configuration
     * @param cacheName     the name of the {@link Cache}. Names should not use
     *                      forward slashes(/) or colons(:), or start with
     *                      java. or javax. These prefixes are reserved.
     * @param configuration a {@link Configuration} for the {@link Cache}
     * @throws IllegalStateException         if the {@link CacheManager}
     *                                       {@link #isClosed()}
     * @throws CacheException                if there was an error configuring the
     *                                       {@link Cache}, which includes trying
     *                                       to create a cache that already exists.
     * @throws IllegalArgumentException      if the configuration is invalid
     * @throws UnsupportedOperationException if the configuration specifies
     *                                       an unsupported feature
     * @throws NullPointerException          if the cache configuration or name
     *                                       is null
     * @throws SecurityException             when the operation could not be performed
     *                                       due to the current security settings
     */
    @Override
    public <K, V, C extends Configuration<K, V>> Cache<K, V> createCache(String cacheName, C configuration) throws IllegalArgumentException {
        assertNotClosed();
        if (cacheRepository.getOrDefault(cacheName, null) != null) {
            throw new CacheException(format("The Cache whose name is '%s' is already existed, " +
                    "please try another name to create a new Cache.", cacheName));
        }
        // If a Cache with the specified name is unknown the CacheManager, one is created according to
        // the provided Configuration after which it becomes managed by the CacheManager.
        return getOrCreateCache(cacheName, configuration, true);
    }

    private <K, V, C extends Configuration<K, V>> Cache<K, V> getOrCreateCache(String cacheName, C configuration, boolean created) {
        CacheIdentifier cacheIdentifier = null;
        CacheIdentifier identifier = cacheRepository.getOrDefault(cacheName, null);
        if (identifier == null && created) {
            cacheIdentifier = new CacheIdentifier();
            // TODO 如果keytype 和valuetype 不同，可以添加不同的Cache ，这里就取一个了
            KeyValueTypePair keyValueTypePair = new KeyValueTypePair(configuration.getKeyType(), configuration.getValueType());
            cacheIdentifier.keyValueTypePair = keyValueTypePair;
            cacheIdentifier.cache = doCreateCache(cacheName, configuration);
            cacheRepository.putIfAbsent(cacheName, cacheIdentifier);

        }
        return cacheIdentifier.cache;
    }

    //扩展
    protected abstract <K, V, C extends Configuration<K, V>> Cache doCreateCache(String cacheName, C configuration);

    @Override
    public <K, V> Cache<K, V> getCache(String cacheName, Class<K> keyType, Class<V> valueType) {
        MutableConfiguration<K, V> configuration = new MutableConfiguration<K, V>().setTypes(keyType, valueType);
        return getOrCreateCache(cacheName, configuration, false);
    }

    @Override
    public <K, V> Cache<K, V> getCache(String cacheName) {
        CacheIdentifier cacheIdentifier = cacheRepository.get(cacheName);
        if (cacheIdentifier == null) {
            return null;
        }
        return (Cache<K, V>) cacheIdentifier.cache;
    }

    @Override
    public Iterable<String> getCacheNames() {
        assertNotClosed();
        return cacheRepository.keySet();
    }

    @Override
    public void destroyCache(String cacheName) {
        requireNonNull(cacheName, "The 'cacheName' argument must not be null.");
        assertNotClosed();
        CacheIdentifier cacheIdentifier = cacheRepository.remove(cacheName);
        if (cacheIdentifier != null) {
            //TODO 如果cache 很多，需要迭代执行
            cacheIdentifier.cache.close();
        }
        // 添加了一个扩展的方式
        doClose();
    }

    /**
     * Subclass may override this method
     */
    protected void doClose() {
    }


    @Override
    public void enableManagement(String cacheName, boolean enabled) {
        //TODO 添加JMX功能加以管理
    }

    @Override
    public void enableStatistics(String cacheName, boolean enabled) {
        //TODO 配合JMX功能添加统计
    }

    @Override
    public void close() {
        isClosed = true;
    }

    @Override
    public boolean isClosed() {
        return isClosed;
    }

    @Override
    public <T> T unwrap(Class<T> clazz) {
        T value = null;
        try {
            value = clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return value;
    }

    /* @throws IllegalStateException    if the CacheManager is {@link #isClosed()} */
    private void assertNotClosed() throws IllegalStateException {
        if (isClosed()) {
            throw new IllegalStateException("The CacheManager has been closed, current operation should not be invoked!");
        }
    }
}
