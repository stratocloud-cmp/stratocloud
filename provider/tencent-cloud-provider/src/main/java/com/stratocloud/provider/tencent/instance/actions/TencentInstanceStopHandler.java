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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public class TencentInstanceStopHandler implements ResourceActionHandler {

    private final TencentInstanceHandler instanceHandler;

    public TencentInstanceStopHandler(TencentInstanceHandler instanceHandler) {
        this.instanceHandler = instanceHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return instanceHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.STOP;
    }

    @Override
    public String getTaskName() {
        return "云主机关机";
    }

    @Override
    public Set<ResourceState> getAllowedStates() {
        return Set.of(ResourceState.STARTED);
    }

    @Override
    public Optional<ResourceState> getTransitionState() {
        return Optional.of(ResourceState.STOPPING);
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return StopInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        StopInput stopInput = JSON.convert(parameters, StopInput.class);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) instanceHandler.getProvider();

        ExternalResource instance
                = instanceHandler.describeExternalResource(account, resource.getExternalId()).orElseThrow();

        if(instance.isStoppedOrShutdown()) {
            log.info("Instance {} is already in {} state, skipping stop action...", instance.name(), instance.state());
            return;
        }

        provider.buildClient(account).stopInstance(resource.getExternalId(), stopInput.getStopType());
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
    public static class StopInput implements ResourceActionInput{
        @SelectField(
                label = "关闭模式",
                options = {"SOFT", "SOFT_FIRST", "HARD"},
                optionNames = {"软关机", "优先软关机，失败再执行硬关机", "硬关机"},
                defaultValues = "SOFT"
        )
        private String stopType;
    }
}
