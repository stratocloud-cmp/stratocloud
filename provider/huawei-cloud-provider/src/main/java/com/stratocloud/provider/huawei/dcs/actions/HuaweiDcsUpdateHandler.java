package com.stratocloud.provider.huawei.dcs.actions;

import com.huaweicloud.sdk.dcs.v2.model.InstanceListInfo;
import com.huaweicloud.sdk.dcs.v2.model.ModifyInstanceBody;
import com.huaweicloud.sdk.dcs.v2.model.UpdateInstanceRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.InputField;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.common.HuaweiCloudClient;
import com.stratocloud.provider.huawei.dcs.HuaweiDcsHandler;
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
public class HuaweiDcsUpdateHandler implements ResourceActionHandler {

    private final HuaweiDcsHandler dcsHandler;

    public HuaweiDcsUpdateHandler(HuaweiDcsHandler dcsHandler) {
        this.dcsHandler = dcsHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return dcsHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.UPDATE;
    }

    @Override
    public String getTaskName() {
        return "更新Redis实例信息";
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

        Optional<InstanceListInfo> instance = dcsHandler.describeInstance(account, resource.getExternalId());

        if(instance.isEmpty())
            return Optional.empty();

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(UpdateInput.class);

        UpdateInput updateInput = new UpdateInput();
        updateInput.setInstanceName(instance.get().getName());

        formMetaData = DynamicFormHelper.changeDefaultValues(
                formMetaData,
                updateInput
        );

        return Optional.of(formMetaData);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        HuaweiCloudProvider provider = (HuaweiCloudProvider) dcsHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        HuaweiCloudClient client = provider.buildClient(account);

        UpdateInput input = JSON.convert(parameters, UpdateInput.class);

        UpdateInstanceRequest request = new UpdateInstanceRequest();
        request.setInstanceId(resource.getExternalId());
        ModifyInstanceBody body = new ModifyInstanceBody();
        body.setName(input.getInstanceName());
        request.setBody(body);

        client.dcs().updateInstance(request);
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
