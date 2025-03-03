package com.stratocloud.provider.aliyun.eip.requirements;

import com.aliyun.vpc20160428.models.AssociateEipAddressRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.eip.AliyunEip;
import com.stratocloud.provider.aliyun.eip.AliyunEipHandler;
import com.stratocloud.provider.aliyun.nic.AliyunNic;
import com.stratocloud.provider.aliyun.nic.AliyunNicHandler;
import com.stratocloud.provider.relationship.ExclusiveRequirementHandler;
import com.stratocloud.provider.relationship.PrimaryCapabilityHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.resource.Relationship;
import com.stratocloud.resource.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class AliyunEipToNicHandler implements ExclusiveRequirementHandler, PrimaryCapabilityHandler {

    private final AliyunEipHandler eipHandler;

    private final AliyunNicHandler nicHandler;

    public AliyunEipToNicHandler(AliyunEipHandler eipHandler,
                                 AliyunNicHandler nicHandler) {
        this.eipHandler = eipHandler;
        this.nicHandler = nicHandler;
    }


    @Override
    public String getRelationshipTypeId() {
        return "ALIYUN_EIP_TO_NIC_RELATIONSHIP";
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
        AliyunCloudProvider provider = (AliyunCloudProvider) eipHandler.getProvider();

        AliyunNic aliyunNic = nicHandler.describeNic(account, nic.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Aliyun nic not found.")
        );

        AssociateEipAddressRequest request = new AssociateEipAddressRequest();
        request.setAllocationId(eip.getExternalId());

        if(aliyunNic.isPrimaryNic()){
            request.setInstanceType("EcsInstance");
            request.setInstanceId(aliyunNic.detail().getInstanceId());
        } else {
            request.setInstanceType("NetworkInterface");
            request.setInstanceId(aliyunNic.detail().getNetworkInterfaceId());
        }

        provider.buildClient(account).vpc().associateEip(request);
    }

    @Override
    public void disconnect(Relationship relationship) {
        Resource eip = relationship.getSource();

        ExternalAccount account = getAccountRepository().findExternalAccount(eip.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) eipHandler.getProvider();

        provider.buildClient(account).vpc().disassociateEip(eip.getExternalId());
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account,
                                                                  ExternalResource source) {
        Optional<AliyunEip> eip = eipHandler.describeEip(account, source.externalId());

        if(eip.isEmpty())
            return List.of();

        String bindResourceType = eip.get().detail().getInstanceType();

        String networkInterfaceId;
        if(Objects.equals("EcsInstance", bindResourceType)){

            AliyunCloudProvider provider = (AliyunCloudProvider) eipHandler.getProvider();

            Optional<AliyunNic> primaryNic = provider.buildClient(account).ecs().describePrimaryNicByInstanceId(
                    eip.get().detail().getInstanceId()
            );

            if(primaryNic.isEmpty())
                return List.of();

            networkInterfaceId = primaryNic.get().detail().getNetworkInterfaceId();
        } else if(Objects.equals("NetworkInterface", bindResourceType)){
            networkInterfaceId = eip.get().detail().getInstanceId();
        } else {
            return List.of();
        }


        Optional<ExternalResource> nic = nicHandler.describeExternalResource(account, networkInterfaceId);

        if(nic.isEmpty())
            return List.of();

        return List.of(new ExternalRequirement(
                getRelationshipTypeId(),
                nic.get(),
                Map.of()
        ));
    }
}
