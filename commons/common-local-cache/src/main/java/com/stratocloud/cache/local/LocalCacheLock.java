package com.stratocloud.cache.local;

import com.stratocloud.cache.CacheLock;
import com.stratocloud.utils.concurrent.SleepUtil;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;


public class LocalCacheLock implements CacheLock {

    private final String lockName;

    private final LocalCache localCache;

    public LocalCacheLock(String lockName, LocalCache localCache) {
        this.lockName = lockName;
        this.localCache = localCache;
    }

    @Override
    public String getName() {
        return lockName;
    }

    @Override
    public boolean tryLock(int waitSeconds, int lockSeconds) {
        int interval = 1;
        int count = 0;
        boolean locked;
        while (!(locked = tryLock(lockSeconds)) && (count * interval)<waitSeconds){
            SleepUtil.sleep(1);
            count++;
        }

        return locked;
    }

    @Override
    public boolean tryLock(int lockSeconds){
        synchronized (localCache){
            String s = localCache.get(lockName, String.class);
            if(s == null) {
                LocalDateTime expireTime = LocalDateTime.now().plus(Duration.of(lockSeconds, ChronoUnit.SECONDS));
                localCache.set(lockName, "", expireTime);
                return true;
            }else {
                return false;
            }
        }
    }

    @Override
    public void unlock() {
        localCache.remove(lockName);
    }
}
