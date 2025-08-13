package com.stratocloud.provider.huawei.obs.actions;

import com.obs.services.model.AvailableZoneEnum;
import com.obs.services.model.CreateBucketRequest;
import com.obs.services.model.ObsBucket;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.common.HuaweiCloudClient;
import com.stratocloud.provider.huawei.obs.HuaweiBucketHandler;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class HuaweiBucketBuildHandler implements BuildResourceActionHandler {

    private final HuaweiBucketHandler bucketHandler;

    public HuaweiBucketBuildHandler(HuaweiBucketHandler bucketHandler) {
        this.bucketHandler = bucketHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return bucketHandler;
    }

    @Override
    public String getTaskName() {
        return "创建存储桶";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return HuaweiBucketBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        HuaweiBucketBuildInput input = JSON.convert(parameters, HuaweiBucketBuildInput.class);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) bucketHandler.getProvider();
        HuaweiCloudClient client = provider.buildClient(account);

        CreateBucketRequest request = new CreateBucketRequest(resource.getName());
        request.setLocation(client.getRegionId());
        request.setAcl(input.getCannedACL());
        request.setBucketStorageClass(input.getStorageClass());
        request.setAvailableZone(input.isEnableMultiAz() ? AvailableZoneEnum.MULTI_AZ : null);
        request.setBucketType(input.getBucketType());

        ObsBucket bucket = client.obs().createBucket(request);
        resource.setExternalId(bucket.getBucketName());

        input.applyVersioningQuietly(client, bucket.getBucketName());
        input.applyEncryptionQuietly(client, bucket.getBucketName());
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) bucketHandler.getProvider();
        HuaweiCloudClient client = provider.buildClient(account);

        if(client.obs().doesBucketExist(resource.getName()))
            throw new BadCommandException("存储桶名称 %s 已存在".formatted(resource.getName()));
    }
}
