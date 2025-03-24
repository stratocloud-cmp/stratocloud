package com.stratocloud.provider.huawei.nic.requirements;

import com.huaweicloud.sdk.vpc.v2.model.Port;
import com.huaweicloud.sdk.vpc.v2.model.Subnet;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.nic.HuaweiNicHandler;
import com.stratocloud.provider.huawei.subnet.HuaweiSubnetHandler;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiNicToSubnetHandler implements EssentialRequirementHandler {

    private final HuaweiNicHandler nicHandler;

    private final HuaweiSubnetHandler subnetHandler;

    public HuaweiNicToSubnetHandler(HuaweiNicHandler nicHandler,
                                    HuaweiSubnetHandler subnetHandler) {
        this.nicHandler = nicHandler;
        this.subnetHandler = subnetHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "HUAWEI_NIC_TO_SUBNET_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "子网与网卡";
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
        return "网卡";
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
        Optional<Port> port = nicHandler.describePort(account, source.externalId());

        if(port.isEmpty())
            return List.of();

        String networkId = port.get().getNetworkId();

        // Huawei subnet is actually neutron network.
        Optional<Subnet> subnet = subnetHandler.describeSubnetByNeutronNetworkId(account, networkId);

        return subnet.map(s -> List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        subnetHandler.toExternalResource(account, s),
                        Map.of()
                )
        )).orElseGet(List::of);

    }
}
