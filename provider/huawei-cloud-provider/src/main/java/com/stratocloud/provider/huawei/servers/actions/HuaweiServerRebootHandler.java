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
public class HuaweiServerRebootHandler implements ResourceActionHandler {

    private final HuaweiServerHandler serverHandler;

    public HuaweiServerRebootHandler(HuaweiServerHandler serverHandler) {
        this.serverHandler = serverHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return serverHandler;
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
        return ResourceState.getAliveStateSet();
    }

    @Override
    public Optional<ResourceState> getTransitionState() {
        return Optional.of(ResourceState.RESTARTING);
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return RebootInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) serverHandler.getProvider();

        RebootInput input = JSON.convert(parameters, RebootInput.class);

        ExternalResource server
                = serverHandler.describeExternalResource(account, resource.getExternalId()).orElseThrow();

        provider.buildClient(account).ecs().rebootServerAndWait(server.externalId(), input.isHardReboot());
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        var server = serverHandler.describeExternalResource(account, resource.getExternalId());

        if(server.isEmpty())
            return ResourceActionResult.failed("Server not found.");

        if(server.get().isStarted())
            return ResourceActionResult.finished();

        return ResourceActionResult.failed("Failed to reboot server. Status=%s.".formatted(server.get().state()));
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }


    @Data
    public static class RebootInput implements ResourceActionInput {
        @BooleanField(label = "强制重启")
        private boolean hardReboot;
    }
}
