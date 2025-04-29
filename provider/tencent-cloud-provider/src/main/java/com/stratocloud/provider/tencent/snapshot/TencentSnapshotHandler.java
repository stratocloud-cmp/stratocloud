package com.stratocloud.provider.tencent.snapshot;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.cbs.v20170312.models.DescribeSnapshotsRequest;
import com.tencentcloudapi.cbs.v20170312.models.Snapshot;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentSnapshotHandler extends AbstractResourceHandler {

    private final TencentCloudProvider provider;

    public TencentSnapshotHandler(TencentCloudProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "TENCENT_SNAPSHOT";
    }

    @Override
    public String getResourceTypeName() {
        return "腾讯云硬盘快照";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.DISK_SNAPSHOT;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return describeSnapshot(account, externalId).map(
                s -> toExternalResource(account, s)
        );
    }

    public Optional<Snapshot> describeSnapshot(ExternalAccount account, String externalId){
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).describeSnapshot(externalId);
    }

    public ExternalResource toExternalResource(ExternalAccount account, Snapshot snapshot){
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                snapshot.getSnapshotId(),
                snapshot.getSnapshotName(),
                convertState(snapshot.getSnapshotState())
        );
    }

    private ResourceState convertState(String snapshotState) {
        if(Utils.isBlank(snapshotState))
            return ResourceState.UNKNOWN;
        return switch (snapshotState){
            case "NORMAL" -> ResourceState.AVAILABLE;
            case "CREATING", "COPYING_FROM_REMOTE", "CHECKING_COPIED" -> ResourceState.BUILDING;
            case "ROLLBACKING" -> ResourceState.ATTACHING;
            case "TORECYCLE" -> ResourceState.SHUTDOWN;
            default -> ResourceState.UNKNOWN;
        };
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        return provider.buildClient(account).describeSnapshots(
                new DescribeSnapshotsRequest()
        ).stream().map(s -> toExternalResource(account, s)).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Snapshot snapshot = describeSnapshot(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Snapshot not found")
        );

        resource.updateByExternal(toExternalResource(account, snapshot));

        RuntimeProperty createTimeProperty = RuntimeProperty.ofDisplayInList(
                "createTime",
                "快照创建时间",
                snapshot.getCreateTime(),
                snapshot.getCreateTime()
        );
        resource.addOrUpdateRuntimeProperty(createTimeProperty);

        RuntimeProperty deadlineTimeProperty = RuntimeProperty.ofDisplayInList(
                "deadlineTime",
                "快照到期时间",
                snapshot.getDeadlineTime(),
                snapshot.getDeadlineTime()
        );
        resource.addOrUpdateRuntimeProperty(deadlineTimeProperty);
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
