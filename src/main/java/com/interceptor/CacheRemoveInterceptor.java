package com.interceptor;

import com.factory.CachingProviderFactory;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.annotation.CacheRemove;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.net.URI;

@Interceptor
public class CacheRemoveInterceptor extends AnnotatedInterceptor<CacheRemove> {

    CachingProvider cachingProvider = CachingProviderFactory.getCachingProvider();
    CacheManager cacheManager = cachingProvider.getCacheManager(URI.create("in-memory://localhost/"), null);
    MutableConfiguration<String, Integer> config =
            new MutableConfiguration<String, Integer>()
                    .setManagementEnabled(true)
                    .setStatisticsEnabled(true)
                    .setTypes(String.class, Integer.class);

    @Override
    protected Object execute(InvocationContext context, CacheRemove cacheRemove) throws Throwable {
        if (cacheRemove == null) {
            return context.proceed();
        }
        Cache cache = getCache(cacheRemove.cacheName());
        boolean afterInvocation = cacheRemove.afterInvocation();
        Object result = context.proceed();
        if (afterInvocation) {
            cache.remove(context.getParameters()[0]);
        }
        return result;
    }


    private Cache getCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            System.out.println("cache not found!!");
        }
        return cache;
    }
}
