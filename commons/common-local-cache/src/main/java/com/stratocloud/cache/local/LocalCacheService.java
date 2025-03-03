package com.stratocloud.cache.local;

import com.stratocloud.cache.CacheLock;
import com.stratocloud.cache.CacheService;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.TemporalUnit;

@Component
public class LocalCacheService implements CacheService {

    private static final LocalCache cache = LocalCache.getInstance();

    @Override
    public <V> V get(String key, Class<V> clazz) {
        return cache.get(key, clazz);
    }

    @Override
    public <V> void set(String key, V value) {
        cache.set(key, value);
    }

    @Override
    public <V> void set(String key, V value, long timeToLive, TemporalUnit unit) {
        cache.set(key, value, LocalDateTime.now().plus(Duration.of(timeToLive, unit)));
    }

    @Override
    public void renew(String key, long timeToLive, TemporalUnit unit) {
        cache.renew(key, LocalDateTime.now().plus(Duration.of(timeToLive, unit)));
    }

    @Override
    public void remove(String key) {
        cache.remove(key);
    }

    @Override
    public void clearAll() {
        cache.clearAll();
    }

    @Override
    public CacheLock getLock(String key) {
        return new LocalCacheLock(key, cache);
    }

    @PreDestroy
    public void shutDown(){
        cache.shutDown();
    }


}
