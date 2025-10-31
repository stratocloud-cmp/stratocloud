package com.stratocloud.provider.aliyun.kafka.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.aliyun.kafka.AliyunKafkaHandler;
import com.stratocloud.provider.aliyun.kafka.KafkaInstance;
import com.stratocloud.provider.constants.DbActions;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class AliyunKafkaIsolateHandler implements ResourceActionHandler {

    private final AliyunKafkaHandler kafkaHandler;

    public AliyunKafkaIsolateHandler(AliyunKafkaHandler kafkaHandler) {
        this.kafkaHandler = kafkaHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return kafkaHandler;
    }

    @Override
    public ResourceAction getAction() {
        return DbActions.ISOLATE;
    }

    @Override
    public String getTaskName() {
        return "释放Kafka实例";
    }

    @Override
    public Set<ResourceState> getAllowedStates() {
        return ResourceState.getAliveStateSet();
    }

    @Override
    public Optional<ResourceState> getTransitionState() {
        return Optional.of(ResourceState.STOPPING);
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

        client.kafka().releaseInstance(resource.getExternalId());
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<ExternalResource> kafka = kafkaHandler.describeExternalResource(account, resource.getExternalId());

        if(kafka.isEmpty())
            return ResourceActionResult.finished();

        if(kafka.get().state() == ResourceState.STOPPING)
            return ResourceActionResult.inProgress();

        return ResourceActionResult.finished();
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }
}
