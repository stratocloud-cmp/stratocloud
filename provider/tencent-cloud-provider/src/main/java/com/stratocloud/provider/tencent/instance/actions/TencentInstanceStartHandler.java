package com.stratocloud.provider.tencent.instance.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.instance.TencentInstanceHandler;
import com.stratocloud.provider.tencent.instance.TencentInstanceUtil;
import com.stratocloud.resource.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public class TencentInstanceStartHandler implements ResourceActionHandler {

    private final TencentInstanceHandler instanceHandler;

    public TencentInstanceStartHandler(TencentInstanceHandler instanceHandler) {
        this.instanceHandler = instanceHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return instanceHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.START;
    }

    @Override
    public String getTaskName() {
        return "云主机开机";
    }

    @Override
    public Set<ResourceState> getAllowedStates() {
        return Set.of(ResourceState.STOPPED);
    }

    @Override
    public Optional<ResourceState> getTransitionState() {
        return Optional.of(ResourceState.STARTING);
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) instanceHandler.getProvider();

        ExternalResource instance
                = instanceHandler.describeExternalResource(account, resource.getExternalId()).orElseThrow();

        if(instance.isStarted()) {
            log.info("Instance {} is already in {} state, skipping start action...", instance.name(), instance.state());
            return;
        }

        provider.buildClient(account).startInstance(resource.getExternalId());
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
}
