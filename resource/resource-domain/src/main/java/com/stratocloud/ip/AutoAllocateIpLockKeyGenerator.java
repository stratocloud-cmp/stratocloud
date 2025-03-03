package com.stratocloud.ip;

import com.stratocloud.lock.DistributedLockKeyGenerator;
import com.stratocloud.resource.Resource;
import com.stratocloud.utils.Utils;

public class AutoAllocateIpLockKeyGenerator implements DistributedLockKeyGenerator {
    @Override
    public String generateKey(String lockName, Object[] args) {
        if(Utils.isEmpty(args))
            return lockName;

        if(!(args[0] instanceof Resource networkResource))
            return lockName;

        return lockName+"_"+networkResource.getId();
    }
}
