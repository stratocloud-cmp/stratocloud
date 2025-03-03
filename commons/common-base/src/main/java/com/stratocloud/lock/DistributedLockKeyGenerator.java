package com.stratocloud.lock;


public interface DistributedLockKeyGenerator {
    String generateKey(String lockName, Object[] args);
}
