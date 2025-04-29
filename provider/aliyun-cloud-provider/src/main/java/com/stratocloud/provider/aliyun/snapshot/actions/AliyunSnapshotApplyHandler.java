package com.stratocloud.provider.aliyun.snapshot.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.aliyun.snapshot.AliyunSnapshotHandler;
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
public class AliyunSnapshotApplyHandler implements ResourceActionHandler {

    private final AliyunSnapshotHandler snapshotHandler;

    public AliyunSnapshotApplyHandler(AliyunSnapshotHandler snapshotHandler) {
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
        rollbackToSnapshot(resource, false);
    }

    private void rollbackToSnapshot(Resource resource, boolean dryRun) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) snapshotHandler.getProvider();
        AliyunClient client = provider.buildClient(account);

        var snapshot = snapshotHandler.describeSnapshot(account, resource.getExternalId()).orElseThrow(
                () -> new StratoException("Snapshot not found")
        );

        client.ecs().rollbackToSnapshot(
                snapshot.detail().getSourceDiskId(),
                snapshot.detail().getSnapshotId(),
                dryRun
        );
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
        rollbackToSnapshot(resource, true);
    }
}
