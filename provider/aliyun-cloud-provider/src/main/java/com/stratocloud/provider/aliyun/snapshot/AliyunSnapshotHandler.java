package com.stratocloud.provider.aliyun.snapshot;

import com.aliyun.ecs20140526.models.DescribeSnapshotsRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AliyunSnapshotHandler extends AbstractResourceHandler {

    private final AliyunCloudProvider provider;

    public AliyunSnapshotHandler(AliyunCloudProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "ALIYUN_SNAPSHOT";
    }

    @Override
    public String getResourceTypeName() {
        return "阿里云硬盘快照";
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

    public Optional<AliyunSnapshot> describeSnapshot(ExternalAccount account, String externalId){
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).ecs().describeSnapshot(externalId);
    }

    public ExternalResource toExternalResource(ExternalAccount account, AliyunSnapshot snapshot){
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                snapshot.detail().getSnapshotId(),
                snapshot.detail().getSnapshotName(),
                convertState(snapshot.detail().getStatus())
        );
    }

    private ResourceState convertState(String status) {
        if(Utils.isBlank(status))
            return ResourceState.UNKNOWN;
        return switch (status){
            case "accomplished" -> ResourceState.AVAILABLE;
            case "progressing" -> ResourceState.BUILDING;
            case "failed" -> ResourceState.BUILD_ERROR;
            default -> ResourceState.UNKNOWN;
        };
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        return provider.buildClient(account).ecs().describeSnapshots(
                new DescribeSnapshotsRequest()
        ).stream().map(s -> toExternalResource(account, s)).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunSnapshot snapshot = describeSnapshot(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Snapshot not found")
        );

        resource.updateByExternal(toExternalResource(account, snapshot));

        RuntimeProperty createTimeProperty = RuntimeProperty.ofDisplayInList(
                "createTime",
                "快照创建时间",
                snapshot.detail().getCreationTime(),
                snapshot.detail().getCreationTime()
        );
        resource.addOrUpdateRuntimeProperty(createTimeProperty);
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
