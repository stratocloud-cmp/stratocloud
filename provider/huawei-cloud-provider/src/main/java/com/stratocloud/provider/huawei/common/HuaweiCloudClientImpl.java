package com.stratocloud.provider.huawei.common;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.huaweicloud.sdk.core.auth.BasicCredentials;
import com.huaweicloud.sdk.core.auth.GlobalCredentials;
import com.huaweicloud.sdk.ecs.v2.model.NovaAvailabilityZone;
import com.huaweicloud.sdk.iam.v3.model.ProjectResult;
import com.stratocloud.cache.CacheService;
import com.stratocloud.cache.CacheUtil;
import com.stratocloud.exceptions.ExternalAccountInvalidException;
import com.stratocloud.provider.huawei.common.services.*;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

@Slf4j
public class HuaweiCloudClientImpl implements HuaweiCloudClient {

    private final CacheService cacheService;

    private final BasicCredentials credential;

    private final String regionId;

    public HuaweiCloudClientImpl(CacheService cacheService, HuaweiCloudAccountProperties accountProperties) {
        turnOffAccessLog();

        this.cacheService = cacheService;
        this.credential = new BasicCredentials().withAk(
                accountProperties.getAccessKeyId()
        ).withSk(
                accountProperties.getSecretAccessKey()
        );
        this.regionId = accountProperties.getRegion();
    }

    private static void turnOffAccessLog() {
        ILoggerFactory iLoggerFactory = LoggerFactory.getILoggerFactory();
        if(iLoggerFactory instanceof LoggerContext loggerContext){
            loggerContext.getLogger("HuaweiCloud-SDK-Access").setLevel(Level.OFF);
        }
    }


    @Override
    public String getRegionId() {
        return regionId;
    }

    @Override
    public synchronized String getProjectId() {
        if(Utils.isNotBlank(credential.getProjectId()))
            return credential.getProjectId();

        return CacheUtil.queryWithCache(
                cacheService,
                "HuaweiProjectIdOfAccessKey-%s-AndOfRegionId-%s".formatted(
                        credential.getAk(),
                        regionId
                ),
                3000L,
                this::doGetProjectId,
                ""
        );
    }

    private String doGetProjectId() {
        String projectId = credential.getProjectId();

        if(Utils.isBlank(projectId)) {
            Optional<ProjectResult> project = iam().getProject();

            if(project.isPresent())
                projectId = project.get().getId();
        }

        credential.setProjectId(projectId);

        return projectId;
    }

    @Override
    public void validateConnection() {
        List<NovaAvailabilityZone> zones = ecs().describeZones();

        if(Utils.isEmpty(zones))
            throw new ExternalAccountInvalidException("No available zone.");
    }

    @Override
    public Float describeBalance() {
        return bss().describeBalance();
    }

    @Override
    public HuaweiEcsService ecs() {
        return new HuaweiEcsServiceImpl(cacheService, credential, regionId, credential.getAk());
    }

    @Override
    public HuaweiVpcService vpc() {
        return new HuaweiVpcServiceImpl(cacheService, credential, regionId, credential.getAk());
    }

    @Override
    public HuaweiEvsService evs() {
        return new HuaweiEvsServiceImpl(cacheService, credential, regionId, credential.getAk());
    }

    @Override
    public HuaweiImsService ims() {
        return new HuaweiImsServiceImpl(cacheService, credential, regionId, credential.getAk());
    }

    @Override
    public HuaweiBssService bss() {
        var globalCredentials = new GlobalCredentials().withAk(credential.getAk()).withSk(credential.getSk());

        return new HuaweiBssServiceImpl(cacheService, globalCredentials, regionId, credential.getAk());
    }

    @Override
    public HuaweiIamService iam(){
        return new HuaweiIamServiceImpl(cacheService, credential, regionId, credential.getAk());
    }

    @Override
    public HuaweiEipService eip(){
        return new HuaweiEipServiceImpl(cacheService, credential, regionId, credential.getAk());
    }

    @Override
    public HuaweiKpsService kps(){
        return new HuaweiKpsServiceImpl(cacheService, credential, regionId, credential.getAk());
    }

    @Override
    public HuaweiElbService elb(){
        return new HuaweiElbServiceImpl(cacheService, credential, regionId, credential.getAk());
    }

    @Override
    public HuaweiCocService coc(){
        return new HuaweiCocServiceImpl(cacheService, credential, regionId, credential.getAk());
    }

    @Override
    public HuaweiCesService ces(){
        return new HuaweiCesServiceImpl(cacheService, credential, regionId, credential.getAk());
    }
}
