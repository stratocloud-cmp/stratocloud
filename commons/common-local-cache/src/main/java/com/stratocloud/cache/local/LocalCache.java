package com.stratocloud.cache.local;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class LocalCache {
    private final Map<String, CacheValue<?>> map = new ConcurrentHashMap<>();

    private static final Duration defaultExpireDuration = Duration.ofSeconds(30);

    private static final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    private static final LocalCache instance = new LocalCache();


    private LocalCache(){
        Runnable expireWorker = () -> {
            LocalDateTime now = LocalDateTime.now();
            map.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expireTime()));
        };

        scheduledExecutorService.scheduleWithFixedDelay(expireWorker,0,1, TimeUnit.SECONDS);
    }
    public static LocalCache getInstance(){
        return instance;
    }

    public <V> void set(String key, V value, LocalDateTime expireTime){
        map.put(key, new CacheValue<>(value, expireTime));
    }

    public <V> void set(String key, V value){
        CacheValue<V> cacheValue = new CacheValue<>(value, LocalDateTime.now().plus(defaultExpireDuration));
        map.put(key, cacheValue);
    }

    @SuppressWarnings("unchecked")
    public <V> V get(String key, Class<V> clazz){
        CacheValue<?> cacheValue = map.get(key);

        if(cacheValue == null)
            return null;

        if(!clazz.isAssignableFrom(cacheValue.value().getClass()))
            return null;

        return (V) cacheValue.value();
    }

    public void renew(String key, LocalDateTime expireTime) {
        CacheValue<?> cacheValue = map.get(key);

        if(cacheValue == null)
            return;

        map.put(key, new CacheValue<>(cacheValue.value(), expireTime));
    }

    public void clearAll() {
        map.clear();
    }

    public void remove(String key) {
        map.remove(key);
    }

    public void shutDown() {
        scheduledExecutorService.shutdownNow();
    }

    public record CacheValue<V>(V value, LocalDateTime expireTime) {
    }
}
