package com.stratocloud.provider.huawei.disk.requirements;

import com.huaweicloud.sdk.evs.v2.model.Attachment;
import com.huaweicloud.sdk.evs.v2.model.VolumeDetail;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.disk.HuaweiDiskHandler;
import com.stratocloud.provider.huawei.servers.HuaweiServerHandler;
import com.stratocloud.provider.relationship.EssentialPrimaryCapabilityHandler;
import com.stratocloud.provider.relationship.ExclusiveRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Relationship;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiSystemDiskToServerHandler
        implements ExclusiveRequirementHandler, EssentialPrimaryCapabilityHandler {

    private final HuaweiDiskHandler diskHandler;

    private final HuaweiServerHandler serverHandler;

    public HuaweiSystemDiskToServerHandler(HuaweiDiskHandler diskHandler,
                                           HuaweiServerHandler serverHandler) {
        this.diskHandler = diskHandler;
        this.serverHandler = serverHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "HUAWEI_SYSTEM_DISK_TO_SERVER_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "云主机与系统盘";
    }

    @Override
    public ResourceHandler getSource() {
        return diskHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return serverHandler;
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
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        Optional<VolumeDetail> disk = diskHandler.describeDisk(account, source.externalId());

        if(disk.isEmpty())
            return List.of();

        boolean bootable = "true".equals(disk.get().getBootable());

        if(!bootable)
            return List.of();

        List<Attachment> attachments = disk.get().getAttachments();

        if(Utils.isEmpty(attachments))
            return List.of();

        List<ExternalRequirement> result = new ArrayList<>();

        for (Attachment attachment : attachments) {
            if(Utils.isBlank(attachment.getServerId()))
                continue;

            Optional<ExternalResource> server = serverHandler.describeExternalResource(
                    account, attachment.getServerId()
            );

            if(server.isEmpty())
                continue;

            result.add(new ExternalRequirement(
                    getRelationshipTypeId(),
                    server.get(),
                    Map.of()
            ));
        }

        return result;
    }
}
