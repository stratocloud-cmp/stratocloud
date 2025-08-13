package com.stratocloud.provider.aliyun.oss.actions;

import com.aliyun.oss.model.Bucket;
import com.aliyun.oss.model.CreateBucketRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.aliyun.oss.AliyunBucketHandler;
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
public class AliyunBucketBuildHandler implements BuildResourceActionHandler {

    private final AliyunBucketHandler bucketHandler;

    public AliyunBucketBuildHandler(AliyunBucketHandler bucketHandler) {
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
        return AliyunBucketBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        AliyunBucketBuildInput input = JSON.convert(parameters, AliyunBucketBuildInput.class);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) bucketHandler.getProvider();
        AliyunClient client = provider.buildClient(account);

        CreateBucketRequest request = new CreateBucketRequest(resource.getName());
        request.setCannedACL(input.getAclType());
        request.setStorageClass(input.getStorageClass());
        request.setDataRedundancyType(input.getRedundancyType());

        Bucket bucket = client.oss().createBucket(request);
        resource.setExternalId(bucket.getName());

        input.applyVersioningQuietly(client, bucket.getName());
        input.applyEncryptionQuietly(client, bucket.getName());
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) bucketHandler.getProvider();
        AliyunClient client = provider.buildClient(account);

        if(client.oss().doesBucketExist(resource.getName()))
            throw new BadCommandException("存储桶名称 %s 已存在".formatted(resource.getName()));
    }
}
