package com.stratocloud.provider.huawei.rocket.actions;

import com.huaweicloud.sdk.rocketmq.v2.model.UpdateInstanceReq;
import com.huaweicloud.sdk.rocketmq.v2.model.UpdateInstanceRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.InputField;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.common.HuaweiCloudClient;
import com.stratocloud.provider.huawei.rocket.HuaweiRocketMqHandler;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class HuaweiRocketMqUpdateHandler implements ResourceActionHandler {

    private final HuaweiRocketMqHandler rocketMqHandler;

    public HuaweiRocketMqUpdateHandler(HuaweiRocketMqHandler rocketMqHandler) {
        this.rocketMqHandler = rocketMqHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return rocketMqHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.UPDATE;
    }

    @Override
    public String getTaskName() {
        return "更新RocketMQ实例基本信息";
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
        return UpdateInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        var rocketInstance = rocketMqHandler.describeInstance(account, resource.getExternalId());

        if(rocketInstance.isEmpty())
            return Optional.empty();

        UpdateInput input = new UpdateInput();
        input.setInstanceName(rocketInstance.get().getName());

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(UpdateInput.class);
        formMetaData = DynamicFormHelper.changeDefaultValues(formMetaData, input);

        return Optional.of(formMetaData);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) rocketMqHandler.getProvider();
        HuaweiCloudClient client = provider.buildClient(account);

        UpdateInput input = JSON.convert(parameters, UpdateInput.class);

        UpdateInstanceRequest request = new UpdateInstanceRequest();
        request.setInstanceId(resource.getExternalId());
        request.setBody(new UpdateInstanceReq().withName(input.getInstanceName()));

        client.rocket().updateInstance(request);
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<ExternalResource> rocket = rocketMqHandler.describeExternalResource(account, resource.getExternalId());

        if(rocket.isEmpty())
            return ResourceActionResult.finished();

        if(rocket.get().state() == ResourceState.CONFIGURING)
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

    @Data
    public static class UpdateInput implements ResourceActionInput {
        @InputField(label = "实例名称")
        private String instanceName;
    }
}
