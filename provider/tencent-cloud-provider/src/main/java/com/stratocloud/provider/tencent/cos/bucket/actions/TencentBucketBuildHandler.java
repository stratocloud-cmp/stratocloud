package com.stratocloud.provider.tencent.cos.bucket.actions;

import com.qcloud.cos.model.Bucket;
import com.qcloud.cos.model.CreateBucketRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.cos.bucket.TencentBucketHandler;
import com.stratocloud.provider.tencent.cos.bucket.TencentBucketSpec;
import com.stratocloud.provider.tencent.cos.session.CosSession;
import com.stratocloud.provider.tencent.cos.session.CosSessionKey;
import com.stratocloud.provider.tencent.cos.session.CosSessionManager;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class TencentBucketBuildHandler implements BuildResourceActionHandler {

    private final TencentBucketHandler bucketHandler;

    public TencentBucketBuildHandler(TencentBucketHandler bucketHandler) {
        this.bucketHandler = bucketHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return bucketHandler;
    }

    @Override
    public String getTaskName() {
        return "创建Bucket";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return TencentBucketBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        TencentBucketBuildInput input = JSON.convert(parameters, TencentBucketBuildInput.class);

        TencentCloudProvider provider = (TencentCloudProvider) bucketHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudClient client = provider.buildClient(account);
        CosSessionKey sessionKey = client.getCosSessionKey();
        CosSession cosSession = CosSessionManager.getSession(sessionKey);

        String bucketName = resource.getName() + "-" + client.getUserAppId().getAppId();
        CreateBucketRequest request = new CreateBucketRequest(bucketName);
        request.setCannedAcl(input.getAclType());

        Bucket bucket = cosSession.createBucket(request, input.isEnableMultiAz());
        resource.setExternalId(bucket.getName());

        TencentBucketSpec spec = input.toSpec();
        spec.applyVersioningQuietly(cosSession, bucket.getName());
        spec.applyIntelligentTierQuietly(cosSession, bucket.getName());
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }
}
