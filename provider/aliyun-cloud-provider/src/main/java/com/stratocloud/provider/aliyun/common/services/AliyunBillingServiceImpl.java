package com.stratocloud.provider.aliyun.common.services;

import com.aliyun.bssopenapi20171214.Client;
import com.aliyun.teaopenapi.models.Config;
import com.stratocloud.cache.CacheService;
import com.stratocloud.exceptions.ExternalAccountInvalidException;

public class AliyunBillingServiceImpl extends AliyunAbstractService implements AliyunBillingService {

    public AliyunBillingServiceImpl(CacheService cacheService, Config config) {
        super(cacheService, config);
    }

    private Client buildClient(){
        try {
            return new Client(config);
        } catch (Exception e) {
            throw new ExternalAccountInvalidException(e.getMessage(), e);
        }
    }

    @Override
    public Float describeBalance(){
        String availableAmount = tryInvoke(
                () -> buildClient().queryAccountBalance()
        ).getBody().getData().getAvailableAmount();
        return Float.valueOf(availableAmount);
    }
}
