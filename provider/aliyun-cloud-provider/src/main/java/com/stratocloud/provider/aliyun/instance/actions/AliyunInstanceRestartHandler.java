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
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class AliyunInstanceRestartHandler implements ResourceActionHandler {

    private final AliyunInstanceHandler instanceHandler;

    public AliyunInstanceRestartHandler(AliyunInstanceHandler instanceHandler) {
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
        AliyunCloudProvider provider = (AliyunCloudProvider) instanceHandler.getProvider();
        provider.buildClient(account).ecs().restartInstance(resource.getExternalId(), restartInput.getForceStop());
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<AliyunInstance> instance = instanceHandler.describeInstance(account, resource.getExternalId());

        if(instance.isEmpty())
            return ResourceActionResult.failed("Instance not found.");

        String status = instance.get().detail().getStatus();

        if(Set.of("Starting", "Stopping").contains(status))
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

    @Data
    public static class RestartInput implements ResourceActionInput{
        @BooleanField(
                label = "强制关机"
        )
        private Boolean forceStop;
    }
}
