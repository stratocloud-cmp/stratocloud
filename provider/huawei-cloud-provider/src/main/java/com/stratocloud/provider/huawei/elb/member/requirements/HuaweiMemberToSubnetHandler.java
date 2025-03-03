package com.stratocloud.provider.huawei.elb.member.requirements;

import com.huaweicloud.sdk.vpc.v2.model.Subnet;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.elb.member.HuaweiLbPoolMemberHandler;
import com.stratocloud.provider.huawei.elb.member.HuaweiMember;
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
public class HuaweiMemberToSubnetHandler implements EssentialRequirementHandler {

    private final HuaweiLbPoolMemberHandler memberHandler;

    private final HuaweiSubnetHandler subnetHandler;

    public HuaweiMemberToSubnetHandler(HuaweiLbPoolMemberHandler memberHandler,
                                       HuaweiSubnetHandler subnetHandler) {
        this.memberHandler = memberHandler;
        this.subnetHandler = subnetHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "HUAWEI_MEMBER_TO_SUBNET_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "后端服务器组成员与子网";
    }

    @Override
    public ResourceHandler getSource() {
        return memberHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return subnetHandler;
    }

    @Override
    public String getCapabilityName() {
        return "后端服务器组成员";
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
        Optional<HuaweiMember> member = memberHandler.describeMember(account, source.externalId());

        if(member.isEmpty())
            return List.of();

        String subnetCidrId = member.get().detail().getSubnetCidrId();

        Optional<Subnet> subnet = subnetHandler.describeSubnetByNeutronSubnetId(account, subnetCidrId);

        if(subnet.isEmpty()){
            subnet = subnetHandler.describeSubnetByNeutronNetworkId(account, subnetCidrId);
        }

        return subnet.map(value -> List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        subnetHandler.toExternalResource(account, value),
                        Map.of()
                )
        )).orElseGet(List::of);
    }

    @Override
    public boolean visibleInTarget() {
        return false;
    }
}
