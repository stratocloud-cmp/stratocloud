package com.stratocloud.provider.aliyun.disk.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.disk.AliyunDisk;
import com.stratocloud.provider.aliyun.disk.AliyunDiskHandler;
import com.stratocloud.provider.aliyun.instance.AliyunInstanceHandler;
import com.stratocloud.provider.relationship.EssentialPrimaryCapabilityHandler;
import com.stratocloud.provider.relationship.ExclusiveRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Relationship;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AliyunSystemDiskToInstanceHandler
        implements ExclusiveRequirementHandler, EssentialPrimaryCapabilityHandler {

    private final AliyunDiskHandler diskHandler;

    private final AliyunInstanceHandler instanceHandler;

    public AliyunSystemDiskToInstanceHandler(AliyunDiskHandler diskHandler,
                                             AliyunInstanceHandler instanceHandler) {
        this.diskHandler = diskHandler;
        this.instanceHandler = instanceHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "ALIYUN_SYSTEM_DISK_TO_INSTANCE_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "系统盘与云主机";
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
        return "系统盘";
    }

    @Override
    public String getRequirementName() {
        return "云主机(系统盘)";
    }

    @Override
    public String getConnectActionName() {
        return "挂载系统盘";
    }

    @Override
    public String getDisconnectActionName() {
        return "解除挂载";
    }

    @Override
    public void connect(Relationship relationship) {

    }

    @Override
    public void disconnect(Relationship relationship) {

    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account,
                                                                  ExternalResource source) {
        Optional<AliyunDisk> disk = diskHandler.describeDisk(account, source.externalId());

        if(disk.isEmpty())
            return List.of();

        String diskType = disk.get().detail().getType();
        if(!"system".equalsIgnoreCase(diskType))
            return List.of();

        String instanceId = disk.get().detail().getInstanceId();

        if(Utils.isBlank(instanceId))
            return List.of();

        Optional<ExternalResource> instance = instanceHandler.describeExternalResource(account, instanceId);

        if(instance.isEmpty())
            return List.of();

        return List.of(new ExternalRequirement(
                getRelationshipTypeId(),
                instance.get(),
                Map.of()
        ));
    }
}
