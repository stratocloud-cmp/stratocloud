package com.stratocloud.lock;

public class DefaultDistributedLockKeyGenerator implements DistributedLockKeyGenerator{
    @Override
    public String generateKey(String lockName, Object[] args) {
        return lockName;
    }
}
