package com.stratocloud.provider.huawei.rocket.actions;

import com.huaweicloud.sdk.rocketmq.v2.model.InstanceDetail;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.rocket.HuaweiRocketMqHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiRocketMqDestroyHandler implements DestroyResourceActionHandler {

    private final HuaweiRocketMqHandler rocketMqHandler;

    public HuaweiRocketMqDestroyHandler(HuaweiRocketMqHandler rocketMqHandler) {
        this.rocketMqHandler = rocketMqHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return rocketMqHandler;
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
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<InstanceDetail> instance = rocketMqHandler.describeInstance(account, resource.getExternalId());

        if(instance.isEmpty())
            return;

        HuaweiCloudProvider provider = (HuaweiCloudProvider) rocketMqHandler.getProvider();
        provider.buildClient(account).rocket().deleteInstance(instance.get().getInstanceId());
    }
}
