package com.stratocloud.provider.tencent.instance.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.BooleanField;
import com.stratocloud.form.InputField;
import com.stratocloud.provider.RuntimePropertiesUtil;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.instance.TencentInstanceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.tencentcloudapi.cvm.v20170312.models.Instance;
import com.tencentcloudapi.cvm.v20170312.models.ResetInstancesPasswordRequest;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class TencentInstanceResetPasswordHandler implements ResourceActionHandler {

    private final TencentInstanceHandler instanceHandler;

    public TencentInstanceResetPasswordHandler(TencentInstanceHandler instanceHandler) {
        this.instanceHandler = instanceHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return instanceHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.RESET_PASSWORD;
    }

    @Override
    public String getTaskName() {
        return "重置密码";
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
        return ResetInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ResetInput input = JSON.convert(parameters, ResetInput.class);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Instance instance = instanceHandler.describeInstance(account, resource.getExternalId()).orElseThrow(
                () -> new StratoException("Instance not found when resetting password")
        );

        ResetInstancesPasswordRequest request = new ResetInstancesPasswordRequest();
        request.setInstanceIds(new String[]{instance.getInstanceId()});
        request.setPassword(input.getPassword());
        request.setForceStop(input.isForceStop());

        TencentCloudProvider provider = (TencentCloudProvider) instanceHandler.getProvider();

        provider.buildClient(account).resetInstancesPassword(request);

        RuntimePropertiesUtil.setManagementPassword(resource, input.getPassword());
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
        ResetInput input = JSON.convert(parameters, ResetInput.class);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        ExternalResource instance = instanceHandler.describeExternalResource(account, resource.getExternalId()).orElseThrow(
                () -> new StratoException("Instance not found when resetting password")
        );

        if(!input.isForceStop() && !instance.isStoppedOrShutdown())
            throw new BadCommandException("未指定强制关机，请先关机再重置密码");
    }

    @Data
    public static class ResetInput implements ResourceActionInput {
        @InputField(label = "新密码", inputType = "password")
        private String password;
        @BooleanField(label = "强制关机")
        private boolean forceStop;
    }

}
