package com.stratocloud.provider.tencent.rocketmq.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.rocketmq.TencentRocketHandler;
import com.stratocloud.resource.Resource;
import com.tencentcloudapi.trocket.v20230308.models.InstanceItem;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class TencentRocketDestroyHandler implements DestroyResourceActionHandler {

    private final TencentRocketHandler rocketHandler;

    public TencentRocketDestroyHandler(TencentRocketHandler rocketHandler) {
        this.rocketHandler = rocketHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return rocketHandler;
    }

    @Override
    public String getTaskName() {
        return "销毁RocketMQ实例";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        TencentCloudProvider provider = (TencentCloudProvider) rocketHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudClient client = provider.buildClient(account);

        Optional<InstanceItem> rocket = rocketHandler.describeRocket(account, resource.getExternalId());

        if(rocket.isEmpty())
            return;

        client.destroyRocketInstance(rocket.get().getInstanceId());
    }
}
