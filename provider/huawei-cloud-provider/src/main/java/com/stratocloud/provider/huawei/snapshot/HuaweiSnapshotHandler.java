package com.stratocloud.provider.huawei.snapshot;

import com.huaweicloud.sdk.evs.v2.model.ListSnapshotsRequest;
import com.huaweicloud.sdk.evs.v2.model.SnapshotList;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiSnapshotHandler extends AbstractResourceHandler {

    private final HuaweiCloudProvider provider;

    public HuaweiSnapshotHandler(HuaweiCloudProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "HUAWEI_SNAPSHOT";
    }

    @Override
    public String getResourceTypeName() {
        return "华为云硬盘快照";
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

    public Optional<SnapshotList> describeSnapshot(ExternalAccount account, String externalId){
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).evs().describeSnapshot(externalId);
    }

    public ExternalResource toExternalResource(ExternalAccount account, SnapshotList snapshot){
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                snapshot.getId(),
                snapshot.getName(),
                convertState(snapshot.getStatus())
        );
    }

    private ResourceState convertState(String status) {
        if(Utils.isBlank(status))
            return ResourceState.UNKNOWN;
        return switch (status){
            case "available" -> ResourceState.AVAILABLE;
            case "creating" -> ResourceState.BUILDING;
            case "rollbacking", "backing-up" -> ResourceState.ATTACHING;
            case "deleting" -> ResourceState.DESTROYING;
            case "error", "error_deleting" -> ResourceState.ERROR;
            default -> ResourceState.UNKNOWN;
        };
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        return provider.buildClient(account).evs().describeSnapshots(
                new ListSnapshotsRequest()
        ).stream().map(s -> toExternalResource(account, s)).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        SnapshotList snapshot = describeSnapshot(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Snapshot not found")
        );

        resource.updateByExternal(toExternalResource(account, snapshot));

        RuntimeProperty createTimeProperty = RuntimeProperty.ofDisplayInList(
                "createTime",
                "快照创建时间",
                snapshot.getCreatedAt(),
                snapshot.getCreatedAt()
        );
        resource.addOrUpdateRuntimeProperty(createTimeProperty);
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
