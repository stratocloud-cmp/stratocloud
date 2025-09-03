package com.stratocloud.provider.tencent.redis.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.form.BooleanField;
import com.stratocloud.form.InputField;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.redis.TencentRedisHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.tencentcloudapi.redis.v20180412.models.ResetPasswordRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public class TencentRedisResetPasswordHandler implements ResourceActionHandler {

    private final TencentRedisHandler redisHandler;

    public TencentRedisResetPasswordHandler(TencentRedisHandler redisHandler) {
        this.redisHandler = redisHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return redisHandler;
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
        TencentCloudProvider provider = (TencentCloudProvider) redisHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudClient client = provider.buildClient(account);

        ResetInput input = JSON.convert(parameters, ResetInput.class);

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setInstanceId(resource.getExternalId());

        request.setNoAuth(input.isNoAuth());
        request.setPassword(input.isNoAuth() ? null : input.getPassword());

        client.modifyRedisPassword(request);
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
    public static class ResetInput implements ResourceActionInput {
        @BooleanField(label = "免密码访问")
        private boolean noAuth;
        @InputField(label = "密码", inputType = "password", conditions = "this.noAuth === false")
        private String password;
    }
}
