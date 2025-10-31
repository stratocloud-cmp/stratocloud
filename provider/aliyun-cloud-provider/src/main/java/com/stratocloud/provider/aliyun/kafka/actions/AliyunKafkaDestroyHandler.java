package com.stratocloud.provider.aliyun.kafka.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.aliyun.kafka.AliyunKafkaHandler;
import com.stratocloud.provider.aliyun.kafka.KafkaInstance;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceActionResult;
import com.stratocloud.utils.concurrent.SleepUtil;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class AliyunKafkaDestroyHandler implements DestroyResourceActionHandler {

    private final AliyunKafkaHandler kafkaHandler;

    public AliyunKafkaDestroyHandler(AliyunKafkaHandler kafkaHandler) {
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
        AliyunCloudProvider provider = (AliyunCloudProvider) kafkaHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunClient client = provider.buildClient(account);

        Optional<KafkaInstance> kafkaInstance = kafkaHandler.describeInstance(account, resource.getExternalId());

        if(kafkaInstance.isEmpty())
            return;

        client.kafka().deleteInstance(resource.getExternalId());
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<KafkaInstance> kafkaInstance = kafkaHandler.describeInstance(account, resource.getExternalId());

        if(kafkaInstance.isEmpty()) {
            SleepUtil.sleep(15);
            return DestroyResourceActionHandler.super.checkActionResult(resource, parameters);
        }
        if(Objects.equals(kafkaInstance.get().detail().getViewInstanceStatusCode(), 23))
            return ResourceActionResult.inProgress();

        SleepUtil.sleep(15);
        return DestroyResourceActionHandler.super.checkActionResult(resource, parameters);
    }
}
