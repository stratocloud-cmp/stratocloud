package com.stratocloud.provider.huawei.servers.actions;

import com.huaweicloud.sdk.ecs.v2.model.ResetServerPasswordOption;
import com.huaweicloud.sdk.ecs.v2.model.ResetServerPasswordRequest;
import com.huaweicloud.sdk.ecs.v2.model.ResetServerPasswordRequestBody;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.form.InputField;
import com.stratocloud.provider.RuntimePropertiesUtil;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.servers.HuaweiServerHandler;
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
public class HuaweiServerResetPasswordHandler implements ResourceActionHandler {

    private final HuaweiServerHandler serverHandler;

    public HuaweiServerResetPasswordHandler(HuaweiServerHandler serverHandler) {
        this.serverHandler = serverHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return serverHandler;
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
        return ResetPasswordInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ResetPasswordInput input = JSON.convert(parameters, ResetPasswordInput.class);

        ResetServerPasswordRequest request = new ResetServerPasswordRequest().withServerId(resource.getExternalId()).withBody(
                new ResetServerPasswordRequestBody().withResetPassword(
                        new ResetServerPasswordOption().withNewPassword(input.getNewPassword())
                )
        );

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) serverHandler.getProvider();

        provider.buildClient(account).ecs().resetServerPassword(request);

        RuntimePropertiesUtil.setManagementPassword(resource, input.getNewPassword());
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
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) serverHandler.getProvider();

        boolean resetPasswordSupported = provider.buildClient(account).ecs().isResetPasswordSupported(
                resource.getExternalId()
        );

        if(!resetPasswordSupported)
            throw new BadCommandException("云主机%s不支持重置密码".formatted(resource.getName()));
    }

    @Data
    public static class ResetPasswordInput implements ResourceActionInput {
        @InputField(label = "新密码", inputType = "password")
        private String newPassword;
    }
}
