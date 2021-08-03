import org.junit.Test;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;
import java.net.URI;

import static org.junit.Assert.assertEquals;

public class testMyCache {

    @Test
    public void testMyCache() {
        CachingProvider cachingProvider = Caching.getCachingProvider();
        CacheManager cacheManager = cachingProvider.getCacheManager(URI.create("in-memory://localhost/"), null);
        MutableConfiguration<String, Integer> config =
                new MutableConfiguration<String, Integer>()
                        .setManagementEnabled(true)
                        .setStatisticsEnabled(true)
                        .setTypes(String.class, Integer.class);
        // create the cache
        Cache<String, Integer> cache = cacheManager.createCache("simpleCache", config);
        // cache operations
        String key = "key";
        Integer value1 = 1;
        cache.put("key", value1);
        assertEquals(value1, cache.get("key"));
    }
}
