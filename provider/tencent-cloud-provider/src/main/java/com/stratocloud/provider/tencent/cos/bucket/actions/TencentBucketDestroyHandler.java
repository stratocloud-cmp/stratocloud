package com.stratocloud.provider.tencent.cos.bucket.actions;

import com.qcloud.cos.model.Bucket;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.cos.bucket.TencentBucketHandler;
import com.stratocloud.provider.tencent.cos.session.CosSession;
import com.stratocloud.provider.tencent.cos.session.CosSessionManager;
import com.stratocloud.resource.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class TencentBucketDestroyHandler implements DestroyResourceActionHandler {

    private final TencentBucketHandler bucketHandler;

    public TencentBucketDestroyHandler(TencentBucketHandler bucketHandler) {
        this.bucketHandler = bucketHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return bucketHandler;
    }

    @Override
    public String getTaskName() {
        return "删除存储桶";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<Bucket> bucket = bucketHandler.describeBucket(account, resource.getExternalId());

        if(bucket.isEmpty())
            return;

        TencentCloudProvider provider = (TencentCloudProvider) bucketHandler.getProvider();
        CosSession cosSession = CosSessionManager.getSession(provider.buildClient(account).getCosSessionKey());

        cosSession.deleteBucket(bucket.get().getName());
    }
}
