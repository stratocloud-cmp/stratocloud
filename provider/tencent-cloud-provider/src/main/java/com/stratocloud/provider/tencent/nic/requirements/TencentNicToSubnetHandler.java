package com.stratocloud.provider.tencent.nic.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.nic.TencentNicHandler;
import com.stratocloud.provider.tencent.subnet.TencentSubnetHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.tencentcloudapi.vpc.v20170312.models.NetworkInterface;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentNicToSubnetHandler implements EssentialRequirementHandler {

    private final TencentNicHandler nicHandler;

    private final TencentSubnetHandler subnetHandler;

    public TencentNicToSubnetHandler(TencentNicHandler nicHandler, TencentSubnetHandler subnetHandler) {
        this.nicHandler = nicHandler;
        this.subnetHandler = subnetHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "TENCENT_NIC_TO_SUBNET_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "弹性网卡与子网";
    }

    @Override
    public ResourceHandler getSource() {
        return nicHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return subnetHandler;
    }

    @Override
    public String getCapabilityName() {
        return "弹性网卡";
    }

    @Override
    public String getRequirementName() {
        return "子网";
    }

    @Override
    public String getConnectActionName() {
        return "关联";
    }

    @Override
    public String getDisconnectActionName() {
        return "解除关联";
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        Optional<NetworkInterface> nic = nicHandler.describeNic(account, source.externalId());

        if(nic.isEmpty())
            return List.of();

        Optional<ExternalResource> subnet = subnetHandler.describeExternalResource(account, nic.get().getSubnetId());

        if(subnet.isEmpty())
            return List.of();

        ExternalRequirement subnetRequirement = new ExternalRequirement(
                getRelationshipTypeId(),
                subnet.get(),
                Map.of()
        );

        return List.of(subnetRequirement);
    }
}
