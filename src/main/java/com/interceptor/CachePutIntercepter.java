package com.interceptor;

import com.factory.CachingProviderFactory;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.annotation.CachePut;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.net.URI;

@Interceptor
public class CachePutIntercepter extends AnnotatedInterceptor<CachePut> {

    CachingProvider cachingProvider = CachingProviderFactory.getCachingProvider();
    CacheManager cacheManager = cachingProvider.getCacheManager(URI.create("in-memory://localhost/"), null);
    MutableConfiguration<String, Integer> config =
            new MutableConfiguration<String, Integer>()
                    .setManagementEnabled(true)
                    .setStatisticsEnabled(true)
                    .setTypes(String.class, Integer.class);

    @Override
    protected Object execute(InvocationContext context, CachePut cachePut) throws Throwable {
        if (cachePut == null) {
            return context.proceed();
        }
        String cacheName = cachePut.cacheName();
        Cache cache = getCache(cacheName);
        /* If true and the annotated method throws an exception the rules governing */
        boolean afterInvocation = cachePut.afterInvocation();
        // The result of target method
        Object result = context.proceed();
        if (afterInvocation) {
            Object[] parameters = context.getParameters();
            Object key = parameters[0];
            Object value = parameters[1];
            cache.put(key, value);
        }
        return result;
    }

    private Cache getCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            cache = cacheManager.createCache(cacheName, config);
        }
        return cache;
    }
}
