package com.stratocloud.provider.tencent.database.cdb.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.constants.DbActions;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.database.cdb.TencentCdbHandler;
import com.stratocloud.resource.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class TencentCdbIsolateHandler implements ResourceActionHandler {

    private final TencentCdbHandler cdbHandler;

    public TencentCdbIsolateHandler(TencentCdbHandler cdbHandler) {
        this.cdbHandler = cdbHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return cdbHandler;
    }

    @Override
    public ResourceAction getAction() {
        return DbActions.ISOLATE;
    }

    @Override
    public String getTaskName() {
        return "隔离云数据库";
    }

    @Override
    public Set<ResourceState> getAllowedStates() {
        return ResourceState.getAliveStateSet().stream().filter(
                s -> s != ResourceState.SHUTDOWN
        ).collect(Collectors.toSet());
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
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) cdbHandler.getProvider();
        TencentCloudClient client = provider.buildClient(account);

        Optional<ExternalResource> cdb = cdbHandler.describeExternalResource(account, resource.getExternalId());

        if(cdb.isEmpty())
            return;

        client.isolateCdbInstance(cdb.get().externalId());
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
}
