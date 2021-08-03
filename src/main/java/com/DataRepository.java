package com;

import javax.cache.annotation.CachePut;
import javax.cache.annotation.CacheRemove;

public interface DataRepository {

    @CachePut(cacheName = "simpleCache")
    boolean create(String name, Object value);

    @CacheRemove(cacheName = "simpleCache")
    boolean remove(String name);

    Object get(String name);
}