package com.stratocloud.provider.huawei.snapshot.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.common.HuaweiCloudClient;
import com.stratocloud.provider.huawei.snapshot.HuaweiSnapshotHandler;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class HuaweiSnapshotApplyHandler implements ResourceActionHandler {

    private final HuaweiSnapshotHandler snapshotHandler;

    public HuaweiSnapshotApplyHandler(HuaweiSnapshotHandler snapshotHandler) {
        this.snapshotHandler = snapshotHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return snapshotHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.ROLLBACK_TO_SNAPSHOT;
    }

    @Override
    public String getTaskName() {
        return "回滚快照";
    }

    @Override
    public Set<ResourceState> getAllowedStates() {
        return Set.of(ResourceState.AVAILABLE);
    }

    @Override
    public Optional<ResourceState> getTransitionState() {
        return Optional.empty();
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) snapshotHandler.getProvider();
        HuaweiCloudClient client = provider.buildClient(account);

        var snapshot = snapshotHandler.describeSnapshot(account, resource.getExternalId()).orElseThrow(
                () -> new StratoException("Snapshot not found")
        );

        client.evs().rollbackToSnapshot(
                snapshot.getVolumeId(),
                snapshot.getId()
        );
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<ExternalResource> externalSnapshot
                = snapshotHandler.describeExternalResource(account, resource.getExternalId());

        if(externalSnapshot.isPresent() && externalSnapshot.get().state() == ResourceState.ATTACHING)
            return ResourceActionResult.inProgress();

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
