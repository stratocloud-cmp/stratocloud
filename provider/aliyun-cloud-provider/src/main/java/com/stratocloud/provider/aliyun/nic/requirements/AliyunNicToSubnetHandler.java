package com.stratocloud.provider.aliyun.nic.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.nic.AliyunNic;
import com.stratocloud.provider.aliyun.nic.AliyunNicHandler;
import com.stratocloud.provider.aliyun.subnet.AliyunSubnetHandler;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AliyunNicToSubnetHandler implements EssentialRequirementHandler {

    private final AliyunNicHandler nicHandler;

    private final AliyunSubnetHandler subnetHandler;

    public AliyunNicToSubnetHandler(AliyunNicHandler nicHandler, AliyunSubnetHandler subnetHandler) {
        this.nicHandler = nicHandler;
        this.subnetHandler = subnetHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "ALIYUN_NIC_TO_SUBNET_RELATIONSHIP";
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
        Optional<AliyunNic> nic = nicHandler.describeNic(account, source.externalId());

        if(nic.isEmpty())
            return List.of();

        Optional<ExternalResource> subnet = subnetHandler.describeExternalResource(
                account, nic.get().detail().getVSwitchId()
        );

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
