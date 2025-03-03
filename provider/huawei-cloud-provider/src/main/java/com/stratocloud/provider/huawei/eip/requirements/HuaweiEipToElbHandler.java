package com.stratocloud.provider.huawei.eip.requirements;

import com.huaweicloud.sdk.eip.v2.model.PublicipShowResp;
import com.huaweicloud.sdk.elb.v3.model.LoadBalancer;
import com.huaweicloud.sdk.vpc.v2.model.Port;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.eip.HuaweiEipHandler;
import com.stratocloud.provider.huawei.elb.HuaweiLoadBalancerHandler;
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
public class HuaweiEipToElbHandler implements ExclusiveRequirementHandler, PrimaryCapabilityHandler {

    private final HuaweiEipHandler eipHandler;

    private final HuaweiLoadBalancerHandler loadBalancerHandler;

    public HuaweiEipToElbHandler(HuaweiEipHandler eipHandler,
                                 HuaweiLoadBalancerHandler loadBalancerHandler) {
        this.eipHandler = eipHandler;
        this.loadBalancerHandler = loadBalancerHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "HUAWEI_EIP_TO_ELB_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "负载均衡与弹性IP";
    }

    @Override
    public ResourceHandler getSource() {
        return eipHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return loadBalancerHandler;
    }

    @Override
    public String getCapabilityName() {
        return "弹性IP";
    }

    @Override
    public String getRequirementName() {
        return "负载均衡";
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
        Resource eipResource = relationship.getSource();
        Resource lbResource = relationship.getTarget();

        ExternalAccount account = getAccountRepository().findExternalAccount(eipResource.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) eipHandler.getProvider();

        LoadBalancer lb = loadBalancerHandler.describeLoadBalancer(account, lbResource.getExternalId()).orElseThrow(
                () -> new StratoException("LB not found when associating EIP.")
        );


        provider.buildClient(account).eip().associateEip(
                eipResource.getExternalId(),
                lb.getVipPortId()
        );
    }

    @Override
    public void disconnect(Relationship relationship) {
        Resource eip = relationship.getSource();

        ExternalAccount account = getAccountRepository().findExternalAccount(eip.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) eipHandler.getProvider();

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

        HuaweiCloudProvider provider = (HuaweiCloudProvider) eipHandler.getProvider();

        Optional<Port> port = provider.buildClient(account).vpc().describePort(portId);

        if(port.isEmpty())
            return List.of();

        var lb = loadBalancerHandler.describeExternalResource(account, port.get().getDeviceId());

        return lb.map(externalResource -> List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        externalResource,
                        Map.of()
                )
        )).orElseGet(List::of);

    }
}
