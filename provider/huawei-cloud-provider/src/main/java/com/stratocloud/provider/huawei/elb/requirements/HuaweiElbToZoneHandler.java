package com.stratocloud.provider.huawei.elb.requirements;

import com.huaweicloud.sdk.elb.v3.model.LoadBalancer;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.elb.HuaweiLoadBalancerHandler;
import com.stratocloud.provider.huawei.zone.HuaweiZoneHandler;
import com.stratocloud.provider.relationship.RelationshipHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class HuaweiElbToZoneHandler implements RelationshipHandler {

    public static final String TYPE_ID = "HUAWEI_ELB_TO_ZONE_RELATIONSHIP";
    private final HuaweiLoadBalancerHandler loadBalancerHandler;

    private final HuaweiZoneHandler zoneHandler;

    public HuaweiElbToZoneHandler(HuaweiLoadBalancerHandler loadBalancerHandler,
                                  HuaweiZoneHandler zoneHandler) {
        this.loadBalancerHandler = loadBalancerHandler;
        this.zoneHandler = zoneHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return TYPE_ID;
    }

    @Override
    public String getRelationshipTypeName() {
        return "负载均衡与可用区";
    }

    @Override
    public ResourceHandler getSource() {
        return loadBalancerHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return zoneHandler;
    }

    @Override
    public String getCapabilityName() {
        return "负载均衡";
    }

    @Override
    public String getRequirementName() {
        return "可用区";
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
    public void connect(Relationship relationship) {
        Resource elb = relationship.getSource();
        Resource zone = relationship.getTarget();

        ExternalAccount account = getAccountRepository().findExternalAccount(elb.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) loadBalancerHandler.getProvider();

        provider.buildClient(account).elb().associateElbToZone(
                elb.getExternalId(), zone.getExternalId()
        );
    }

    @Override
    public void disconnect(Relationship relationship) {
        Resource elb = relationship.getSource();
        Resource zone = relationship.getTarget();

        ExternalAccount account = getAccountRepository().findExternalAccount(elb.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) loadBalancerHandler.getProvider();

        Optional<LoadBalancer> lb = loadBalancerHandler.describeLoadBalancer(account, elb.getExternalId());

        if(lb.isEmpty())
            return;

        if(Utils.length(lb.get().getAvailabilityZoneList()) == 1){
            log.warn("Cannot disassociate the only az of elb {}, skipping...", lb.get().getName());
            return;
        }

        provider.buildClient(account).elb().disassociateElbFromZone(
                elb.getExternalId(), zone.getExternalId()
        );
    }

    @Override
    public RelationshipActionResult checkDisconnectResult(ExternalAccount account, Relationship relationship) {
        Resource elb = relationship.getSource();

        Optional<LoadBalancer> lb = loadBalancerHandler.describeLoadBalancer(account, elb.getExternalId());

        if(lb.isEmpty())
            return RelationshipActionResult.finished();

        if(Utils.length(lb.get().getAvailabilityZoneList()) == 1){
            return RelationshipActionResult.finished();
        }

        return RelationshipHandler.super.checkDisconnectResult(account, relationship);
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        Optional<LoadBalancer> elb = loadBalancerHandler.describeLoadBalancer(account, source.externalId());

        if(elb.isEmpty())
            return List.of();

        List<String> zoneCodes = elb.get().getAvailabilityZoneList();

        if(Utils.isEmpty(zoneCodes))
            return List.of();

        List<ExternalResource> zones = zoneHandler.describeExternalResources(account, Map.of()).stream().filter(
                z -> zoneCodes.contains(z.externalId())
        ).toList();

        return zones.stream().map(
                z -> new ExternalRequirement(
                        getRelationshipTypeId(),
                        z,
                        Map.of()
                )
        ).toList();
    }

    @Override
    public boolean requireTargetResourceTaskLock() {
        return true;
    }
}
