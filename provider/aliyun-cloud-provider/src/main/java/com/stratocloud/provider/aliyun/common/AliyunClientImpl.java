package com.stratocloud.provider.aliyun.common;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.aliyun.teaopenapi.models.Config;
import com.stratocloud.cache.CacheService;
import com.stratocloud.exceptions.ProviderConnectionException;
import com.stratocloud.provider.aliyun.common.services.*;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

@Slf4j
public class AliyunClientImpl implements AliyunClient {

    private final CacheService cacheService;

    private final Config config;

    public AliyunClientImpl(AliyunAccountProperties properties, CacheService cacheService) {
        this.cacheService = cacheService;
        this.config = createConfig(properties);

        turnOffModelLog();
    }

    private static void turnOffModelLog() {
        ILoggerFactory iLoggerFactory = LoggerFactory.getILoggerFactory();
        if(iLoggerFactory instanceof LoggerContext loggerContext){
            loggerContext.getLogger("com.aliyun.tea.TeaModel").setLevel(Level.OFF);
        }
    }

    private Config createConfig(AliyunAccountProperties properties) {
        Config c = new Config();
        c.setAccessKeyId(properties.getAccessKeyId());
        c.setAccessKeySecret(properties.getAccessKeySecret());
        c.setRegionId(properties.getRegion());
        return c;
    }

    @Override
    public String getRegionId() {
        return config.getRegionId();
    }

    @Override
    public void validateConnection() {
        var zones = ecs().describeZones();

        if(Utils.isEmpty(zones))
            throw new ProviderConnectionException("No zones found from Aliyun.");
    }

    @Override
    public Float describeBalance() {
        return billing().describeBalance();
    }

    @Override
    public AliyunComputeService ecs(){
        return new AliyunComputeServiceImpl(cacheService, config);
    }

    @Override
    public AliyunNetworkService vpc(){
        return new AliyunNetworkServiceImpl(cacheService, config);
    }

    @Override
    public AliyunBillingService billing(){
        return new AliyunBillingServiceImpl(cacheService, config);
    }


    @Override
    public AliyunClbService clb(){return new AliyunClbServiceImpl(cacheService, config);}

    @Override
    public AliyunCmsService cms() {
        return new AliyunCmsServiceImpl(cacheService, config);
    }

    @Override
    public AliyunTrailService trail(){
        return new AliyunTrailServiceImpl(cacheService, config);
    }
}
