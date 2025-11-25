package com.stratocloud.provider.huawei.dcs.actions;

import com.huaweicloud.sdk.dcs.v2.model.ResetInstancePasswordBody;
import com.huaweicloud.sdk.dcs.v2.model.ResetPasswordRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.form.BooleanField;
import com.stratocloud.form.InputField;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.common.HuaweiCloudClient;
import com.stratocloud.provider.huawei.dcs.HuaweiDcsHandler;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public class HuaweiDcsResetPasswordHandler implements ResourceActionHandler {

    private final HuaweiDcsHandler dcsHandler;

    public HuaweiDcsResetPasswordHandler(HuaweiDcsHandler dcsHandler) {
        this.dcsHandler = dcsHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return dcsHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.RESET_PASSWORD;
    }

    @Override
    public String getTaskName() {
        return "Redis修改密码";
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
        HuaweiCloudProvider provider = (HuaweiCloudProvider) dcsHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudClient client = provider.buildClient(account);

        ResetInput input = JSON.convert(parameters, ResetInput.class);

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setInstanceId(resource.getExternalId());
        ResetInstancePasswordBody body = new ResetInstancePasswordBody();
        body.setNoPasswordAccess(input.isNoPassword());
        if(!input.isNoPassword())
            body.setNewPassword(input.getPassword());
        request.setBody(body);

        client.dcs().resetPassword(request);
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<ExternalResource> redis = dcsHandler.describeExternalResource(account, resource.getExternalId());

        if(redis.isEmpty())
            return ResourceActionResult.finished();

        if(redis.get().state() == ResourceState.CONFIGURING)
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
    public static class ResetInput implements ResourceActionInput {
        @BooleanField(label = "免密码访问")
        private boolean noPassword;
        @InputField(label = "新密码", inputType = "password", conditions = "this.noPassword === false")
        private String password;
    }
}
