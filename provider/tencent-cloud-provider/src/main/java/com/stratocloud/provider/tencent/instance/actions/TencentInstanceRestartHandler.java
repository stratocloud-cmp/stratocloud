package com.stratocloud.provider.tencent.instance.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.instance.TencentInstanceHandler;
import com.stratocloud.provider.tencent.instance.TencentInstanceUtil;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class TencentInstanceRestartHandler implements ResourceActionHandler {

    private final TencentInstanceHandler instanceHandler;

    public TencentInstanceRestartHandler(TencentInstanceHandler instanceHandler) {
        this.instanceHandler = instanceHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return instanceHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.RESTART;
    }

    @Override
    public String getTaskName() {
        return "云主机重启";
    }

    @Override
    public Set<ResourceState> getAllowedStates() {
        return Set.of(ResourceState.STARTED);
    }

    @Override
    public Optional<ResourceState> getTransitionState() {
        return Optional.of(ResourceState.RESTARTING);
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return RestartInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        RestartInput restartInput = JSON.convert(parameters, RestartInput.class);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) instanceHandler.getProvider();
        provider.buildClient(account).restartInstance(resource.getExternalId(), restartInput.getStopType());
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        return TencentInstanceUtil.checkLastOperationStateForAction(instanceHandler, account, resource);
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }

    @Data
    public static class RestartInput implements ResourceActionInput{
        @SelectField(
                label = "关闭模式",
                options = {"SOFT", "SOFT_FIRST", "HARD"},
                optionNames = {"软关机", "优先软关机，失败再执行硬关机", "硬关机"},
                defaultValues = "SOFT"
        )
        private String stopType;
    }
}
