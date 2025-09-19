package com.stratocloud.provider.tencent.rocketmq.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.InputField;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.rocketmq.TencentRocketHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.tencentcloudapi.trocket.v20230308.models.InstanceItem;
import com.tencentcloudapi.trocket.v20230308.models.ModifyInstanceRequest;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class TencentRocketUpdateHandler implements ResourceActionHandler {

    private final TencentRocketHandler rocketHandler;

    public TencentRocketUpdateHandler(TencentRocketHandler rocketHandler) {
        this.rocketHandler = rocketHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return rocketHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.UPDATE;
    }

    @Override
    public String getTaskName() {
        return "更新RocketMQ实例";
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

        Optional<InstanceItem> rocket = rocketHandler.describeRocket(account, resource.getExternalId());

        if(rocket.isEmpty())
            return Optional.empty();

        UpdateInput input = new UpdateInput();
        input.setInstanceName(rocket.get().getInstanceName());

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(UpdateInput.class);
        formMetaData = DynamicFormHelper.changeDefaultValues(formMetaData, input);

        return Optional.of(formMetaData);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        TencentCloudProvider provider = (TencentCloudProvider) rocketHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudClient client = provider.buildClient(account);

        UpdateInput input = JSON.convert(parameters, UpdateInput.class);

        ModifyInstanceRequest request = new ModifyInstanceRequest();
        request.setInstanceId(resource.getExternalId());
        request.setName(input.getInstanceName());

        client.modifyRocketInstance(request);
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        var rocket = rocketHandler.describeExternalResource(account, resource.getExternalId());

        if(rocket.isEmpty())
            return ResourceActionResult.failed("RocketMQ instance does not exist anymore");

        if(rocket.get().state() == ResourceState.CONFIGURING)
            return ResourceActionResult.inProgress();

        if(rocket.get().state() == ResourceState.ERROR)
            return ResourceActionResult.failed("RocketMQ instance is in error state");

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
