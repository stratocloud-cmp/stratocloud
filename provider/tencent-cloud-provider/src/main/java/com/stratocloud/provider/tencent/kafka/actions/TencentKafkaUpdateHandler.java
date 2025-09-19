package com.stratocloud.provider.tencent.kafka.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.kafka.TencentKafkaHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.tencentcloudapi.ckafka.v20190819.models.InstanceDetail;
import com.tencentcloudapi.ckafka.v20190819.models.ModifyInstanceAttributesRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class TencentKafkaUpdateHandler implements ResourceActionHandler {

    private final TencentKafkaHandler kafkaHandler;

    public TencentKafkaUpdateHandler(TencentKafkaHandler kafkaHandler) {
        this.kafkaHandler = kafkaHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return kafkaHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.UPDATE;
    }

    @Override
    public String getTaskName() {
        return "更新Kafka实例";
    }

    @Override
    public Set<ResourceState> getAllowedStates() {
        return ResourceState.getAliveStateSet();
    }

    @Override
    public Optional<ResourceState> getTransitionState() {
        return Optional.of(ResourceState.CONFIGURING);
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return TencentKafkaUpdateInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<InstanceDetail> kafka = kafkaHandler.describeKafka(account, resource.getExternalId());

        if(kafka.isEmpty())
            return Optional.empty();

        TencentKafkaUpdateInput input = new TencentKafkaUpdateInput();
        input.setInstanceName(kafka.get().getInstanceName());

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(TencentKafkaUpdateInput.class);
        formMetaData = DynamicFormHelper.changeDefaultValues(formMetaData, input);

        return Optional.of(formMetaData);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        TencentCloudProvider provider = (TencentCloudProvider) kafkaHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentKafkaUpdateInput input = JSON.convert(parameters, TencentKafkaUpdateInput.class);

        ModifyInstanceAttributesRequest request = new ModifyInstanceAttributesRequest();
        request.setInstanceId(resource.getExternalId());
        request.setInstanceName(input.getInstanceName());

        provider.buildClient(account).modifyKafkaAttributes(request);
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
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
