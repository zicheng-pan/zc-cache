package com.factory;

import javax.cache.Caching;
import javax.cache.spi.CachingProvider;

public class CachingProviderFactory {
    private static CachingProvider cachingProvider = null;

    public static synchronized CachingProvider getCachingProvider() {
        if (cachingProvider == null)
            cachingProvider = Caching.getCachingProvider();
        return cachingProvider;
    }
}
