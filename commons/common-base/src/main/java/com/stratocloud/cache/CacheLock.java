package com.stratocloud.cache;

public interface CacheLock {
    String getName();

    boolean tryLock(int waitSeconds, int lockSeconds);

    boolean tryLock(int lockSeconds);

    void unlock();
}
