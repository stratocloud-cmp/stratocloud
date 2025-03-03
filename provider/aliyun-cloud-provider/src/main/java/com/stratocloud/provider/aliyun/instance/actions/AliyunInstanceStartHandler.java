package com.stratocloud.provider.aliyun.instance.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.instance.AliyunInstance;
import com.stratocloud.provider.aliyun.instance.AliyunInstanceHandler;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class AliyunInstanceStartHandler implements ResourceActionHandler {

    private final AliyunInstanceHandler instanceHandler;

    public AliyunInstanceStartHandler(AliyunInstanceHandler instanceHandler) {
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
        AliyunCloudProvider provider = (AliyunCloudProvider) instanceHandler.getProvider();

        ExternalResource instance
                = instanceHandler.describeExternalResource(account, resource.getExternalId()).orElseThrow();

        if(instance.isStarted()) {
            log.info("Instance {} is already in {} state, skipping start action...", instance.name(), instance.state());
            return;
        }

        provider.buildClient(account).ecs().startInstance(resource.getExternalId());
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<AliyunInstance> instance = instanceHandler.describeInstance(account, resource.getExternalId());

        if(instance.isEmpty())
            return ResourceActionResult.failed("Instance not found.");

        String status = instance.get().detail().getStatus();

        if(Objects.equals("Starting", status))
            return ResourceActionResult.inProgress();

        if(Objects.equals("Stopped", status))
            return ResourceActionResult.failed("Instance is not started.");

        return ResourceActionResult.finished();
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }
}
