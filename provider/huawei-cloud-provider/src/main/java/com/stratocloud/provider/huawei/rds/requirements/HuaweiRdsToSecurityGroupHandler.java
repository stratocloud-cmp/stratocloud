package com.stratocloud.provider.huawei.rds.requirements;

import com.huaweicloud.sdk.rds.v3.model.InstanceResponse;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.rds.HuaweiRdsHandler;
import com.stratocloud.provider.huawei.securitygroup.HuaweiSecurityGroupHandler;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiRdsToSecurityGroupHandler implements EssentialRequirementHandler {

    private final HuaweiRdsHandler rdsHandler;

    private final HuaweiSecurityGroupHandler securityGroupHandler;

    public HuaweiRdsToSecurityGroupHandler(HuaweiRdsHandler rdsHandler, HuaweiSecurityGroupHandler securityGroupHandler) {
        this.rdsHandler = rdsHandler;
        this.securityGroupHandler = securityGroupHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "HUAWEI_RDS_TO_SECURITY_GROUP_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "华为云RDS实例与安全组";
    }

    @Override
    public ResourceHandler getSource() {
        return rdsHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return securityGroupHandler;
    }

    @Override
    public String getCapabilityName() {
        return "RDS实例";
    }

    @Override
    public String getRequirementName() {
        return "安全组";
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
    public boolean visibleInTarget() {
        return false;
    }

    @Override
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        Optional<InstanceResponse> instance = rdsHandler.describeInstance(account, source.externalId());

        if(instance.isEmpty())
            return List.of();

        String securityGroupId = instance.get().getSecurityGroupId();

        Optional<ExternalResource> securityGroup = securityGroupHandler.describeExternalResource(account, securityGroupId);

        return securityGroup.map(s -> List.of(
                new ExternalRequirement(
                        getRelationshipTypeId(),
                        s,
                        Map.of()
                )
        )).orElseGet(List::of);
    }
}
