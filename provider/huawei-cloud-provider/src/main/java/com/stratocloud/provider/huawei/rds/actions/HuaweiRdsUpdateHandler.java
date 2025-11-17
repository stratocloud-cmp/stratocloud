package com.stratocloud.provider.huawei.rds.actions;

import com.huaweicloud.sdk.rds.v3.model.InstanceResponse;
import com.huaweicloud.sdk.rds.v3.model.ModifiyInstanceNameRequest;
import com.huaweicloud.sdk.rds.v3.model.UpdateInstanceNameRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.InputField;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.rds.HuaweiRdsHandler;
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
public class HuaweiRdsUpdateHandler implements ResourceActionHandler {

    private final HuaweiRdsHandler rdsHandler;

    public HuaweiRdsUpdateHandler(HuaweiRdsHandler rdsHandler) {
        this.rdsHandler = rdsHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return rdsHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.UPDATE;
    }

    @Override
    public String getTaskName() {
        return "更新RDS实例";
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

        Optional<InstanceResponse> rdsInstance = rdsHandler.describeInstance(account, resource.getExternalId());

        if(rdsInstance.isEmpty())
            return Optional.empty();

        UpdateInput updateInput = new UpdateInput();
        updateInput.setInstanceName(rdsInstance.get().getName());

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(UpdateInput.class);

        formMetaData = DynamicFormHelper.changeDefaultValues(formMetaData, updateInput);

        return Optional.of(formMetaData);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        UpdateInput input = JSON.convert(parameters, UpdateInput.class);

        HuaweiCloudProvider provider = (HuaweiCloudProvider) rdsHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        UpdateInstanceNameRequest request = new UpdateInstanceNameRequest();
        request.setInstanceId(resource.getExternalId());
        request.setBody(new ModifiyInstanceNameRequest().withName(input.getInstanceName()));

        provider.buildClient(account).rds().modifyInstanceName(request);
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

    @Data
    public static class UpdateInput implements ResourceActionInput {
        @InputField(label = "实例名称")
        private String instanceName;
    }
}
