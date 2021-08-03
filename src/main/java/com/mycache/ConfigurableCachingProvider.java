package com.mycache;

import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.OptionalFeature;
import javax.cache.spi.CachingProvider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.lang.String.format;

public class ConfigurableCachingProvider implements CachingProvider {

    /**
     * The prefix of property name for the mappings of {@link CacheManager}, e.g:
     * <p>
     * javax.cache.CacheManager.mappings.${uri.scheme}=com.acme.SomeSchemeCacheManager
     */
    public static final String CACHE_MANAGER_MAPPINGS_PROPERTY_PREFIX = "javax.cache.CacheManager.mappings.";

    public static final URI DEFAULT_URI = URI.create("in-memory://localhost/");

    private Properties defaultProperties;

    private static final String DEFAULT_PROPERTIES_RESOURCE_NAME = "META-INF/default-caching-provider.properties";

    private ConcurrentMap<String, CacheManager> cacheManagersRepository = new ConcurrentHashMap<>();


    /**
     * Requests a {@link CacheManager} configured according to the implementation
     * specific {@link URI} be made available that uses the provided
     * {@link ClassLoader} for loading underlying classes.
     * <p>
     * Multiple calls to this method with the same {@link URI} and
     * {@link ClassLoader} must return the same {@link CacheManager} instance,
     * except if a previously returned {@link CacheManager} has been closed.
     * <p>
     * Properties are used in construction of a {@link CacheManager} and do not form
     * part of the identity of the CacheManager. i.e. if a second call is made to
     * with the same {@link URI} and {@link ClassLoader} but different properties,
     * the {@link CacheManager} created in the first call is returned.
     * <p>
     * Properties names follow the same scheme as package names.
     * The prefixes {@code java} and {@code javax} are reserved.
     * Properties are passed through and can be retrieved via
     * {@link CacheManager#getProperties()}.
     * Properties within the package scope of a caching implementation may be used for
     * additional configuration.
     *
     * @param uri         an implementation specific URI for the
     *                    {@link CacheManager} (null means use
     *                    {@link #getDefaultURI()})
     * @param classLoader the {@link ClassLoader}  to use for the
     *                    {@link CacheManager} (null means use
     *                    {@link #getDefaultClassLoader()})
     * @param properties  the {@link Properties} for the {@link CachingProvider}
     *                    to create the {@link CacheManager} (null means no
     *                    implementation specific Properties are required)
     * @throws CacheException    when a {@link CacheManager} for the
     *                           specified arguments could not be produced
     * @throws SecurityException when the operation could not be performed
     *                           due to the current security settings
     */
    public CacheManager getCacheManager(URI uri, ClassLoader classLoader, Properties properties) {
        URI actualURI = uri == null ? this.getDefaultURI() : uri;
        ClassLoader clazzLoader = classLoader == null ? this.getDefaultClassLoader() : classLoader;
        // 通过 default properties 来进行扩展
        Properties actualProperties = new Properties(getDefaultProperties());
        if (properties != null && !properties.isEmpty()) {
            actualProperties.putAll(properties);
        }

        // 为了满足如果 uri相同 classLoader 相同，并且properties相同，返回同一个CacheManager 根据这三个生成唯一表示
        String key = generateCacheManagerKey(actualURI, clazzLoader, actualProperties);
        return cacheManagersRepository.computeIfAbsent(key, k -> newCacheManager(actualURI, clazzLoader, actualProperties));
    }

    private CacheManager newCacheManager(URI actualURI, ClassLoader clazzLoader, Properties actualProperties) {
        CacheManager cacheManager = null;
        // 通过自定义的配置properties来加载所有的 CacheManager的实现
        Class<? extends CacheManager> cacheManagerClass = getRelatedCacheManager(actualURI, actualProperties);
        // 通过获取到的 和协议相对应的 cachemanager 实现类 反射生成对象
        Class[] parameterTypes = new Class[]{CachingProvider.class, URI.class, ClassLoader.class, Properties.class};
        Constructor<? extends CacheManager> constructor = null;
        try {
            constructor = cacheManagerClass.getConstructor(parameterTypes);
            cacheManager = constructor.newInstance(this, actualURI, clazzLoader, actualProperties);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cacheManager;
    }

    private Class<? extends CacheManager> getRelatedCacheManager(URI actualURI, Properties actualProperties) {
        String scheme = actualURI.getScheme();
        String propertiesName = CACHE_MANAGER_MAPPINGS_PROPERTY_PREFIX + scheme;
        String className = actualProperties.getProperty(propertiesName);
        if (className == null) {
            throw new IllegalStateException(format("The implementation class name of %s that is the value of property '%s' " +
                    "must be configured in the Properties[%s]", CacheManager.class.getName(), propertiesName, actualProperties));
        }

        Class<? extends CacheManager> cacheManagerclazz = getAndVelidateClass(className);
        return cacheManagerclazz;

    }

    private Class<? extends CacheManager> getAndVelidateClass(String className) {
        Class<?> cacheManagerClass = null;
        try {
            cacheManagerClass = getDefaultClassLoader().loadClass(className);
            if (!CacheManager.class.isAssignableFrom(cacheManagerClass)) {
                throw new ClassCastException(format("The implementation class of %s must extend %s",
                        CacheManager.class.getName(), CacheManager.class.getName()));
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return (Class<? extends CacheManager>) cacheManagerClass;
    }

    private String generateCacheManagerKey(URI actualURI, ClassLoader clazzLoader, Properties actualProperties) {
        StringBuilder keyBuilder = new StringBuilder(actualURI.toString())
//        StringBuilder keyBuilder = new StringBuilder(actualURI.toASCIIString())
                .append("-").append(clazzLoader)
                .append("-").append(actualProperties);
        return keyBuilder.toString();
    }

    public ClassLoader getDefaultClassLoader() {
        ClassLoader classLoader = Caching.getDefaultClassLoader();
        if (classLoader == null) {
            classLoader = this.getClass().getClassLoader();
        }
        return classLoader;
    }

    public URI getDefaultURI() {
        return DEFAULT_URI;
    }

    private Properties loadDefaultProperties() {
        Properties defaultProperties = new Properties();
        ClassLoader classLoader = getDefaultClassLoader();
        // 所有的CacheManager的实现类都通过这个properties配置文件加载
        InputStream resourceAsStream = classLoader.getResourceAsStream(DEFAULT_PROPERTIES_RESOURCE_NAME);
        try {
            defaultProperties.load(resourceAsStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return defaultProperties;
    }

    public Properties getDefaultProperties() {
        if (this.defaultProperties == null) {
            this.defaultProperties = loadDefaultProperties();
        }
        return defaultProperties;
    }

    public CacheManager getCacheManager(URI uri, ClassLoader classLoader) {
        return getCacheManager(uri, classLoader, getDefaultProperties());
    }

    public CacheManager getCacheManager() {
        return getCacheManager(DEFAULT_URI, getDefaultClassLoader(), getDefaultProperties());
    }


    /**
     * Closes all of the {@link CacheManager} instances and associated resources
     * created and maintained by the {@link CachingProvider} across all
     * {@link ClassLoader}s.
     * <p>
     * After closing the {@link CachingProvider} will still be operational.  It
     * may still be used for acquiring {@link CacheManager} instances, though
     * those will now be new.
     *
     * @throws SecurityException when the operation could not be performed
     *                           due to the current security settings
     */
    public void close() {
        this.close(this.getDefaultURI(), this.getDefaultClassLoader());
    }


    /**
     * Closes all {@link CacheManager} instances and associated resources created
     * by the {@link CachingProvider} using the specified {@link ClassLoader}.
     * <p>
     * After closing the {@link CachingProvider} will still be operational.  It
     * may still be used for acquiring {@link CacheManager} instances, though
     * those will now be new for the specified {@link ClassLoader} .
     *
     * @param classLoader the {@link ClassLoader}  to release
     * @throws SecurityException when the operation could not be performed
     *                           due to the current security settings
     */
    public void close(ClassLoader classLoader) {
        this.close(this.getDefaultURI(), this.getDefaultClassLoader());
    }

    /**
     * Closes all {@link CacheManager} instances and associated resources created
     * by the {@link CachingProvider} for the specified {@link URI} and
     * {@link ClassLoader}.
     *
     * @param uri         the {@link URI} to release
     * @param classLoader the {@link ClassLoader}  to release
     * @throws SecurityException when the operation could not be performed
     *                           due to the current security settings
     */
    public void close(URI uri, ClassLoader classLoader) {
        for (CacheManager cacheManager : cacheManagersRepository.values()) {
            if (Objects.equals(cacheManager.getURI(), uri)
                    && Objects.equals(cacheManager.getClassLoader(), classLoader)) {
                cacheManager.close();
            }
        }
    }

    /**
     * Determines whether an optional feature is supported by the
     * {@link CachingProvider}.
     *
     * @param optionalFeature the feature to check for
     * @return true if the feature is supported
     */
    public boolean isSupported(OptionalFeature optionalFeature) {
        return false;
    }
}
