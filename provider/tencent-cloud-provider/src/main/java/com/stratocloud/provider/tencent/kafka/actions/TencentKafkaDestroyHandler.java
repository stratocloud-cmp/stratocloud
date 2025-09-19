package com.stratocloud.provider.tencent.kafka.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.kafka.TencentKafkaHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

@Component
public class TencentKafkaDestroyHandler implements DestroyResourceActionHandler {

    private final TencentKafkaHandler kafkaHandler;

    public TencentKafkaDestroyHandler(TencentKafkaHandler kafkaHandler) {
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
        if(Utils.isBlank(resource.getExternalId()))
            return;

        TencentCloudProvider provider = (TencentCloudProvider) kafkaHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudClient client = provider.buildClient(account);

        var kafkaAttributes = provider.buildClient(account).describeKafkaAttributes(resource.getExternalId());

        if(kafkaAttributes.isEmpty())
            return;

        if(Objects.equals(kafkaAttributes.get().getInstanceChargeType(), "PREPAID")){
            client.destroyKafkaPrepaidInstance(resource.getExternalId());
        }else {
            client.destroyKafkaPostpaidInstance(resource.getExternalId());
        }
    }
}
