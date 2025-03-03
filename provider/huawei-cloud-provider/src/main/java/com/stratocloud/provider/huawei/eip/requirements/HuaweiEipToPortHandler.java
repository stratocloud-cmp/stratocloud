package com.stratocloud.provider.huawei.eip.requirements;

import com.huaweicloud.sdk.eip.v2.model.PublicipShowResp;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.eip.HuaweiEipHandler;
import com.stratocloud.provider.huawei.nic.HuaweiNicHandler;
import com.stratocloud.provider.relationship.ExclusiveRequirementHandler;
import com.stratocloud.provider.relationship.PrimaryCapabilityHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Relationship;
import com.stratocloud.resource.Resource;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiEipToPortHandler implements ExclusiveRequirementHandler, PrimaryCapabilityHandler {

    private final HuaweiEipHandler eipHandler;

    private final HuaweiNicHandler nicHandler;

    public HuaweiEipToPortHandler(HuaweiEipHandler eipHandler,
                                  HuaweiNicHandler nicHandler) {
        this.eipHandler = eipHandler;
        this.nicHandler = nicHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "HUAWEI_EIP_TO_PORT_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "网卡与弹性IP";
    }

    @Override
    public ResourceHandler getSource() {
        return eipHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return nicHandler;
    }

    @Override
    public String getCapabilityName() {
        return "弹性IP";
    }

    @Override
    public String getRequirementName() {
        return "网卡";
    }

    @Override
    public String getConnectActionName() {
        return "绑定";
    }

    @Override
    public String getDisconnectActionName() {
        return "解除绑定";
    }

    @Override
    public void connect(Relationship relationship) {
        Resource eip = relationship.getSource();
        Resource nic = relationship.getTarget();

        ExternalAccount account = getAccountRepository().findExternalAccount(eip.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) nicHandler.getProvider();

        provider.buildClient(account).eip().associateEip(eip.getExternalId(), nic.getExternalId());
    }

    @Override
    public void disconnect(Relationship relationship) {
        Resource eip = relationship.getSource();

        ExternalAccount account = getAccountRepository().findExternalAccount(eip.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) nicHandler.getProvider();

        provider.buildClient(account).eip().disassociateEip(eip.getExternalId());
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        Optional<PublicipShowResp> eip = eipHandler.describeEip(account, source.externalId());

        if(eip.isEmpty())
            return List.of();

        String portId = eip.get().getPortId();

        if(Utils.isBlank(portId))
            return List.of();

        Optional<ExternalResource> port = nicHandler.describeExternalResource(account, portId);

        return port.map(externalResource -> List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        externalResource,
                        Map.of()
                )
        )).orElseGet(List::of);

    }
}
