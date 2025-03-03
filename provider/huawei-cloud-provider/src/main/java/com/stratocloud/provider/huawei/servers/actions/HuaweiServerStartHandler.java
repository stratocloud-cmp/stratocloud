package com.stratocloud.provider.huawei.servers.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.servers.HuaweiServerHandler;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public class HuaweiServerStartHandler implements ResourceActionHandler {

    private final HuaweiServerHandler serverHandler;

    public HuaweiServerStartHandler(HuaweiServerHandler serverHandler) {
        this.serverHandler = serverHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return serverHandler;
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
        HuaweiCloudProvider provider = (HuaweiCloudProvider) serverHandler.getProvider();

        ExternalResource server
                = serverHandler.describeExternalResource(account, resource.getExternalId()).orElseThrow();

        if(server.isStarted()) {
            log.info("Server {} is already in {} state, skipping START action...", server.name(), server.state());
            return;
        }

        provider.buildClient(account).ecs().startServerAndWait(server.externalId());
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<ExternalResource> server = serverHandler.describeExternalResource(account, resource.getExternalId());

        if(server.isEmpty())
            return ResourceActionResult.failed("Server not found.");

        if(server.get().isStarted())
            return ResourceActionResult.finished();


        return ResourceActionResult.failed("Server not started. Status=%s.".formatted(server.get().state()));
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }
}
