package com.stratocloud.provider.huawei.common.services;

import com.huaweicloud.sdk.core.auth.ICredential;
import com.huaweicloud.sdk.iam.v3.IamClient;
import com.huaweicloud.sdk.iam.v3.model.KeystoneListProjectsRequest;
import com.huaweicloud.sdk.iam.v3.model.ProjectResult;
import com.huaweicloud.sdk.iam.v3.region.IamRegion;
import com.stratocloud.cache.CacheService;

import java.util.Optional;

public class HuaweiIamServiceImpl extends HuaweiAbstractService implements HuaweiIamService{
    public HuaweiIamServiceImpl(CacheService cacheService, ICredential credential, String regionId, String accessKeyId) {
        super(cacheService, credential, regionId, accessKeyId);
    }

    private IamClient buildClient(){
        return IamClient.newBuilder()
                .withCredential(credential)
                .withRegion(IamRegion.valueOf(regionId))
                .build();
    }

    @Override
    public Optional<ProjectResult> getProject(){
        return queryAll(
                () -> buildClient().keystoneListProjects(
                        new KeystoneListProjectsRequest().withName(regionId)
                ).getProjects()
        ).stream().findAny();
    }
}
