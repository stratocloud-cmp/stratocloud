package com.stratocloud.provider.aliyun.instance.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.form.BooleanField;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.instance.AliyunInstance;
import com.stratocloud.provider.aliyun.instance.AliyunInstanceHandler;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class AliyunInstanceStopHandler implements ResourceActionHandler {

    private final AliyunInstanceHandler instanceHandler;

    public AliyunInstanceStopHandler(AliyunInstanceHandler instanceHandler) {
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
        AliyunCloudProvider provider = (AliyunCloudProvider) instanceHandler.getProvider();

        ExternalResource instance
                = instanceHandler.describeExternalResource(account, resource.getExternalId()).orElseThrow();

        if(instance.isStoppedOrShutdown()) {
            log.info("Instance {} is already in {} state, skipping stop action...", instance.name(), instance.state());
            return;
        }

        boolean forceStop = stopInput.getForceStop()!=null ? stopInput.getForceStop() : false;

        provider.buildClient(account).ecs().stopInstance(resource.getExternalId(), forceStop);
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<AliyunInstance> instance = instanceHandler.describeInstance(account, resource.getExternalId());

        if(instance.isEmpty())
            return ResourceActionResult.failed("Instance not found.");

        String status = instance.get().detail().getStatus();

        if(Objects.equals("Stopping", status))
            return ResourceActionResult.inProgress();

        if(Objects.equals("Started", status))
            return ResourceActionResult.failed("Instance is not stopped.");

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
    public static class StopInput implements ResourceActionInput{
        @BooleanField(
                label = "强制关机"
        )
        private Boolean forceStop;
    }
}
