package com.stratocloud.provider.tencent.instance.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.BooleanField;
import com.stratocloud.form.InputField;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.instance.TencentInstanceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.tencentcloudapi.cvm.v20170312.models.ModifyInstancesAttributeRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public class TencentInstanceUpdateHandler implements ResourceActionHandler {

    private final TencentInstanceHandler instanceHandler;

    public TencentInstanceUpdateHandler(TencentInstanceHandler instanceHandler) {
        this.instanceHandler = instanceHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return instanceHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.UPDATE;
    }

    @Override
    public String getTaskName() {
        return "更新云主机";
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
    public void run(Resource resource, Map<String, Object> parameters) {
        UpdateInput updateInput = JSON.convert(parameters, UpdateInput.class);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) instanceHandler.getProvider();

        ExternalResource instance = instanceHandler.describeExternalResource(
                account,
                resource.getExternalId()
        ).orElseThrow(
                () -> new StratoException("Instance not found when updating attributes.")
        );

        ModifyInstancesAttributeRequest request = new ModifyInstancesAttributeRequest();

        request.setInstanceIds(new String[]{instance.externalId()});

        if(updateInput.isUpdatingInstanceName())
            request.setInstanceName(updateInput.getInstanceName());

        if(updateInput.isUpdatingHostName()) {
            request.setHostName(updateInput.getHostName());
            request.setAutoReboot(updateInput.isAutoReboot());
        }


        provider.buildClient(account).modifyInstancesAttribute(request);
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
    public static class UpdateInput implements ResourceActionInput{
        @BooleanField(label = "是否更新实例名称")
        private boolean updatingInstanceName;
        @InputField(label = "新实例名称", conditions = "this.updatingInstanceName === true")
        private String instanceName;
        @BooleanField(label = "是否更新操作系统的主机名")
        private boolean updatingHostName;
        @InputField(label = "操作系统的新主机名", conditions = "this.updatingHostName === true")
        private String hostName;
        @BooleanField(label = "是否自动重启", conditions = "this.updatingHostName === true")
        private boolean autoReboot;
    }
}
