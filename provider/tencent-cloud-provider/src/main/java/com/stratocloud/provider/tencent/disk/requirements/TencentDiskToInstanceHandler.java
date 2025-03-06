package com.stratocloud.provider.tencent.disk.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.relationship.ExclusiveRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.disk.TencentDiskHandler;
import com.stratocloud.provider.tencent.instance.TencentInstanceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Relationship;
import com.stratocloud.resource.Resource;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.cbs.v20170312.models.Disk;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentDiskToInstanceHandler implements ExclusiveRequirementHandler {

    private final TencentDiskHandler diskHandler;

    private final TencentInstanceHandler instanceHandler;

    public TencentDiskToInstanceHandler(TencentDiskHandler diskHandler,
                                        TencentInstanceHandler instanceHandler) {
        this.diskHandler = diskHandler;
        this.instanceHandler = instanceHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "TENCENT_DATA_DISK_TO_INSTANCE_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "数据盘与云主机";
    }

    @Override
    public ResourceHandler getSource() {
        return diskHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return instanceHandler;
    }

    @Override
    public String getCapabilityName() {
        return "数据盘";
    }

    @Override
    public String getRequirementName() {
        return "云主机(数据盘)";
    }

    @Override
    public String getConnectActionName() {
        return "挂载数据盘";
    }

    @Override
    public String getDisconnectActionName() {
        return "解除挂载";
    }

    @Override
    public void connect(Relationship relationship) {
        Resource disk = relationship.getSource();
        Resource instance = relationship.getTarget();

        ExternalAccount account = getAccountRepository().findExternalAccount(disk.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) diskHandler.getProvider();

        TencentCloudClient client = provider.buildClient(account);

        client.attachDisk(instance.getExternalId(), disk.getExternalId());
    }

    @Override
    public void disconnect(Relationship relationship) {
        Resource disk = relationship.getSource();
        Resource instance = relationship.getTarget();

        ExternalAccount account = getAccountRepository().findExternalAccount(disk.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) diskHandler.getProvider();

        TencentCloudClient client = provider.buildClient(account);

        client.detachDisk(instance.getExternalId(), disk.getExternalId());
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account,
                                                                  ExternalResource source) {
        Optional<Disk> disk = diskHandler.describeDisk(account, source.externalId());

        if(disk.isEmpty())
            return List.of();

        if(disk.get().getDiskUsage() != null && "SYSTEM_DISK".equalsIgnoreCase(disk.get().getDiskUsage()))
            return List.of();

        String instanceId = disk.get().getInstanceId();

        if(Utils.isBlank(instanceId))
            return List.of();

        Optional<ExternalResource> instance = instanceHandler.describeExternalResource(account, instanceId);

        return instance.map(externalResource -> List.of(new ExternalRequirement(
                getRelationshipTypeId(),
                externalResource,
                Map.of()
        ))).orElseGet(List::of);

    }


    @Override
    public boolean requireTargetResourceTaskLock() {
        return true;
    }
}
