package com.stratocloud.provider.tencent.nic.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.relationship.EssentialPrimaryCapabilityHandler;
import com.stratocloud.provider.relationship.ExclusiveRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.instance.TencentInstanceHandler;
import com.stratocloud.provider.tencent.nic.TencentNicHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Relationship;
import com.tencentcloudapi.vpc.v20170312.models.NetworkInterface;
import com.tencentcloudapi.vpc.v20170312.models.NetworkInterfaceAttachment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentPrimaryNicToInstanceHandler
        implements ExclusiveRequirementHandler, EssentialPrimaryCapabilityHandler {

    private final TencentNicHandler nicHandler;

    private final TencentInstanceHandler instanceHandler;

    public TencentPrimaryNicToInstanceHandler(TencentNicHandler nicHandler,
                                              TencentInstanceHandler instanceHandler) {
        this.nicHandler = nicHandler;
        this.instanceHandler = instanceHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "TENCENT_PRIMARY_NIC_TO_INSTANCE_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "主网卡与云主机";
    }

    @Override
    public ResourceHandler getSource() {
        return nicHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return instanceHandler;
    }

    @Override
    public String getCapabilityName() {
        return "主网卡";
    }

    @Override
    public String getRequirementName() {
        return "云主机(主网卡)";
    }

    @Override
    public String getConnectActionName() {
        return "挂载主网卡";
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
        Optional<NetworkInterface> nic = nicHandler.describeNic(account, source.externalId());

        if(nic.isEmpty())
            return List.of();

        if(nic.get().getPrimary() == null || !nic.get().getPrimary())
            return List.of();

        NetworkInterfaceAttachment attachment = nic.get().getAttachment();

        if(attachment == null)
            return List.of();

        Optional<ExternalResource> instance = instanceHandler.describeExternalResource(
                account, attachment.getInstanceId()
        );

        if(instance.isEmpty())
            return List.of();

        return List.of(new ExternalRequirement(
                getRelationshipTypeId(),
                instance.get(),
                Map.of()
        ));
    }
}
