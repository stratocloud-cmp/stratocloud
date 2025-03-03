package com.stratocloud.provider.aliyun.instance.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.aliyun.instance.AliyunInstance;
import com.stratocloud.provider.aliyun.instance.AliyunInstanceHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class AliyunInstanceDestroyHandler implements DestroyResourceActionHandler {
    private final AliyunInstanceHandler instanceHandler;

    public AliyunInstanceDestroyHandler(AliyunInstanceHandler instanceHandler) {
        this.instanceHandler = instanceHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return instanceHandler;
    }

    @Override
    public String getTaskName() {
        return "销毁云主机";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) instanceHandler.getProvider();

        Optional<AliyunInstance> instance = instanceHandler.describeInstance(account, resource.getExternalId());

        if(instance.isEmpty())
            return;


        AliyunClient client = provider.buildClient(account);
        client.ecs().deleteInstance(instance.get().detail().getInstanceId());
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<AliyunInstance> instance = instanceHandler.describeInstance(account, resource.getExternalId());

        if(instance.isEmpty())
            return;

        if("PrePaid".equalsIgnoreCase(instance.get().detail().getInstanceChargeType()))
            throw new BadCommandException("请先转为按量计费再销毁");
    }
}
