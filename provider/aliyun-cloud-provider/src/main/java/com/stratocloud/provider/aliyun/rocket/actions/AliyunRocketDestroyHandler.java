package com.stratocloud.provider.aliyun.rocket.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.rocket.AliyunRocketHandler;
import com.stratocloud.provider.aliyun.rocket.RocketInstance;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class AliyunRocketDestroyHandler implements DestroyResourceActionHandler {

    private final AliyunRocketHandler rocketHandler;

    public AliyunRocketDestroyHandler(AliyunRocketHandler rocketHandler) {
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
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<RocketInstance> instance = rocketHandler.describeInstance(account, resource.getExternalId());

        if(instance.isEmpty())
            return;

        AliyunCloudProvider provider = (AliyunCloudProvider) rocketHandler.getProvider();

        provider.buildClient(account).rocket().deleteInstance(instance.get().detail().getInstanceId());
    }
}
