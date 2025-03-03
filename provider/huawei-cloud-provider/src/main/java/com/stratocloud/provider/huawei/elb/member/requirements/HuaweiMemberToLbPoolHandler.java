package com.stratocloud.provider.huawei.elb.member.requirements;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.elb.member.HuaweiLbPoolMemberHandler;
import com.stratocloud.provider.huawei.elb.member.HuaweiMember;
import com.stratocloud.provider.huawei.elb.pool.HuaweiLbPoolHandler;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiMemberToLbPoolHandler implements EssentialRequirementHandler {

    private final HuaweiLbPoolMemberHandler memberHandler;

    private final HuaweiLbPoolHandler lbPoolHandler;

    public HuaweiMemberToLbPoolHandler(HuaweiLbPoolMemberHandler memberHandler,
                                       HuaweiLbPoolHandler lbPoolHandler) {
        this.memberHandler = memberHandler;
        this.lbPoolHandler = lbPoolHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "HUAWEI_MEMBER_TO_LB_POOL_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "后端服务器组与成员";
    }

    @Override
    public ResourceHandler getSource() {
        return memberHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return lbPoolHandler;
    }

    @Override
    public String getCapabilityName() {
        return "成员";
    }

    @Override
    public String getRequirementName() {
        return "后端服务器组";
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

        String poolId = member.get().memberId().poolId();
        Optional<ExternalResource> pool = lbPoolHandler.describeExternalResource(account, poolId);

        return pool.map(externalResource -> List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        externalResource,
                        Map.of()
                )
        )).orElseGet(List::of);

    }

    @Override
    public boolean synchronizeTarget() {
        return true;
    }
}
