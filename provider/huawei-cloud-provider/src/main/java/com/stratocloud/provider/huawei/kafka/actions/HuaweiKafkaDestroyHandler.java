package com.stratocloud.provider.huawei.kafka.actions;

import com.huaweicloud.sdk.kafka.v2.model.ShowInstanceResp;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.kafka.HuaweiKafkaHandler;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiKafkaDestroyHandler implements DestroyResourceActionHandler {

    private final HuaweiKafkaHandler kafkaHandler;

    public HuaweiKafkaDestroyHandler(HuaweiKafkaHandler kafkaHandler) {
        this.kafkaHandler = kafkaHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return kafkaHandler;
    }

    @Override
    public String getTaskName() {
        return "销毁Kafka实例";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<ShowInstanceResp> instance = kafkaHandler.describeInstance(account, resource.getExternalId());

        if(instance.isEmpty())
            return;

        HuaweiCloudProvider provider = (HuaweiCloudProvider) kafkaHandler.getProvider();
        provider.buildClient(account).kafka().deleteInstance(instance.get().getInstanceId());
    }
}
