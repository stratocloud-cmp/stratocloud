package com.stratocloud.provider.huawei.obs.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.common.HuaweiCloudClient;
import com.stratocloud.provider.huawei.obs.HuaweiBucketHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class HuaweiBucketDestroyHandler implements DestroyResourceActionHandler {

    private final HuaweiBucketHandler bucketHandler;

    public HuaweiBucketDestroyHandler(HuaweiBucketHandler bucketHandler) {
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
        if(Utils.isBlank(resource.getExternalId()))
            return;

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) bucketHandler.getProvider();
        HuaweiCloudClient client = provider.buildClient(account);

        client.obs().deleteBucket(resource.getExternalId());
    }
}
