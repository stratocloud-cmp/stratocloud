package com.stratocloud.provider.tencent.eip.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.relationship.ExclusiveRequirementHandler;
import com.stratocloud.provider.relationship.PrimaryCapabilityHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.eip.TencentEipHandler;
import com.stratocloud.provider.tencent.nic.TencentNicHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Relationship;
import com.stratocloud.resource.Resource;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.vpc.v20170312.models.Address;
import com.tencentcloudapi.vpc.v20170312.models.NetworkInterface;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentEipToNicHandler implements ExclusiveRequirementHandler, PrimaryCapabilityHandler {

    private final TencentEipHandler eipHandler;

    private final TencentNicHandler nicHandler;

    public TencentEipToNicHandler(TencentEipHandler eipHandler,
                                  TencentNicHandler nicHandler) {
        this.eipHandler = eipHandler;
        this.nicHandler = nicHandler;
    }


    @Override
    public String getRelationshipTypeId() {
        return "TENCENT_EIP_TO_NIC_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "弹性IP与弹性网卡";
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
        return "弹性网卡";
    }

    @Override
    public String getConnectActionName() {
        return "绑定弹性IP";
    }

    @Override
    public String getDisconnectActionName() {
        return "解除绑定";
    }

    @Override
    public void connect(Relationship relationship) {
        Resource nic = relationship.getTarget();
        Resource eip = relationship.getSource();

        ExternalAccount account = getAccountRepository().findExternalAccount(eip.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) eipHandler.getProvider();

        NetworkInterface networkInterface = nicHandler.describeNic(account, nic.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Tencent nic not found.")
        );

        provider.buildClient(account).associateEipToNic(
                eip.getExternalId(),
                nic.getExternalId(),
                getPrimaryIpOrElse(networkInterface)
        );
    }

    private String getPrimaryIpOrElse(NetworkInterface networkInterface) {
        if(Utils.isEmpty(networkInterface.getPrivateIpAddressSet()))
            throw new StratoException(
                    "Tencent nic %s does not have any private ip.".formatted(
                            networkInterface.getNetworkInterfaceId()
                    )
            );

        return Arrays.stream(networkInterface.getPrivateIpAddressSet()).filter(
                ip -> ip.getPrimary() != null && ip.getPrimary()
        ).findAny().orElse(networkInterface.getPrivateIpAddressSet()[0]).getPrivateIpAddress();
    }

    @Override
    public void disconnect(Relationship relationship) {
        Resource eip = relationship.getSource();

        ExternalAccount account = getAccountRepository().findExternalAccount(eip.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) eipHandler.getProvider();

        provider.buildClient(account).disassociateEipFromNic(eip.getExternalId());
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account,
                                                                  ExternalResource source) {
        Optional<Address> address = eipHandler.describeEip(account, source.externalId());

        if(address.isEmpty())
            return List.of();

        String networkInterfaceId = address.get().getNetworkInterfaceId();


        Optional<ExternalResource> nic = nicHandler.describeExternalResource(account, networkInterfaceId);

        return nic.map(externalResource -> List.of(new ExternalRequirement(
                getRelationshipTypeId(),
                externalResource,
                Map.of()
        ))).orElseGet(List::of);

    }
}
