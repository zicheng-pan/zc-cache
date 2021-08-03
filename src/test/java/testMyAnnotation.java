import com.DataRepository;
import com.InMemoryDataRepository;
import com.InterceptorEnhancer;
import com.interceptor.CachePutIntercepter;
import com.interceptor.CacheRemoveInterceptor;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class testMyAnnotation {

    @Test
    public void testAdd() {
        CachePutIntercepter cachePutIntercepter = new CachePutIntercepter();
        CacheRemoveInterceptor cacheRemoveInterceptor = new CacheRemoveInterceptor();
        try {

            InMemoryDataRepository inMemoryDataRepository = new InMemoryDataRepository();

            InterceptorEnhancer enhancer = new InterceptorEnhancer();
            DataRepository enhanceJDKProxy = (DataRepository) enhancer.enhanceJDKProxy(inMemoryDataRepository, cachePutIntercepter, cacheRemoveInterceptor);
            enhanceJDKProxy.create("aaaa", 123);

            boolean removed = enhanceJDKProxy.remove("aaaa");
            assertTrue(removed);


        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }
}
