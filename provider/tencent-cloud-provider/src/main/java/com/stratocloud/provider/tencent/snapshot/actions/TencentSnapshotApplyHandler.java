package com.stratocloud.provider.tencent.snapshot.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.BooleanField;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.snapshot.TencentSnapshotHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.cbs.v20170312.models.Disk;
import com.tencentcloudapi.cvm.v20170312.models.Instance;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TencentSnapshotApplyHandler implements ResourceActionHandler {

    private final TencentSnapshotHandler snapshotHandler;

    public TencentSnapshotApplyHandler(TencentSnapshotHandler snapshotHandler) {
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
        return ApplyInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ApplyInput input = JSON.convert(parameters, ApplyInput.class);
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) snapshotHandler.getProvider();
        TencentCloudClient client = provider.buildClient(account);

        var snapshot = snapshotHandler.describeSnapshot(account, resource.getExternalId()).orElseThrow(
                () -> new StratoException("Snapshot not found")
        );

        Disk disk = client.describeDisk(snapshot.getDiskId()).orElseThrow(
                () -> new StratoException("Disk not found")
        );

        boolean diskAttached = disk.getAttached() != null && disk.getAttached();


        client.rollbackToSnapshot(
                snapshot.getDiskId(),
                snapshot.getSnapshotId(),
                diskAttached ? input.isAutoStop() : null,
                diskAttached ? input.isAutoStart() : null
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
        ApplyInput input = JSON.convert(parameters, ApplyInput.class);
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        var snapshot = snapshotHandler.describeSnapshot(account, resource.getExternalId()).orElseThrow(
                () -> new BadCommandException("该快照已不存在")
        );

        String snapshotState = snapshot.getSnapshotState();
        if(!Objects.equals(snapshotState, "NORMAL"))
            throw new BadCommandException("该快照当前状态%s不支持回滚".formatted(snapshotState));

        TencentCloudProvider provider = (TencentCloudProvider) snapshotHandler.getProvider();
        TencentCloudClient client = provider.buildClient(account);

        Disk disk = client.describeDisk(snapshot.getDiskId()).orElseThrow(
                () -> new BadCommandException("该快照对应的云硬盘已不存在")
        );

        boolean portable = disk.getPortable() != null && disk.getPortable();

        if (!portable) {
            if(Utils.isNotBlank(disk.getInstanceId())){
                Optional<Instance> instance = client.describeInstance(disk.getInstanceId());
                if(instance.isPresent()){
                    if(!input.isAutoStop() && !"STOPPED".equalsIgnoreCase(instance.get().getInstanceState()))
                        throw new BadCommandException("回滚该快照必须关机");
                }
            }
        } else {
            if(!"UNATTACHED".equalsIgnoreCase(disk.getDiskState()))
                throw new BadCommandException("回滚该快照前必须卸载云硬盘");
        }
    }

    @Data
    public static class ApplyInput implements ResourceActionInput {
        @BooleanField(label = "回滚前自动关机", defaultValue = true)
        private boolean autoStop;
        @BooleanField(label = "回滚后自动开机", defaultValue = true)
        private boolean autoStart;
    }
}
