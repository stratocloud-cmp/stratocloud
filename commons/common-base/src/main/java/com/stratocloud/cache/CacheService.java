package com.stratocloud.cache;

import java.time.temporal.TemporalUnit;

public interface CacheService {
    <V> V get(String key, Class<V> clazz);

    <V> void set(String key, V value);

    <V> void set(String key, V value, long timeToLive, TemporalUnit unit);

    void renew(String key, long timeToLive, TemporalUnit unit);

    void remove(String key);

    void clearAll();

    CacheLock getLock(String key);
}
