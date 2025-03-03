package com.stratocloud.provider.huawei.servers.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.form.BooleanField;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.servers.HuaweiServerHandler;
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
public class HuaweiServerStopHandler implements ResourceActionHandler {

    private final HuaweiServerHandler serverHandler;

    public HuaweiServerStopHandler(HuaweiServerHandler serverHandler) {
        this.serverHandler = serverHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return serverHandler;
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
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        StopInput input = JSON.convert(parameters, StopInput.class);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) serverHandler.getProvider();

        ExternalResource server
                = serverHandler.describeExternalResource(account, resource.getExternalId()).orElseThrow();

        if(server.isStoppedOrShutdown()) {
            log.info("Server {} is already in {} state, skipping STOP action...", server.name(), server.state());
            return;
        }

        provider.buildClient(account).ecs().stopServerAndWait(server.externalId(), input.isHardStop());
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        var server = serverHandler.describeExternalResource(account, resource.getExternalId());

        if(server.isEmpty())
            return ResourceActionResult.failed("Server not found.");

        if(server.get().isStoppedOrShutdown())
            return ResourceActionResult.finished();


        return ResourceActionResult.failed("Server not stopped. Status=%s.".formatted(server.get().state()));
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }

    @Data
    public static class StopInput implements ResourceActionInput {
        @BooleanField(label = "强制关机")
        private boolean hardStop;
    }
}
