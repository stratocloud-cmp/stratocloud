package com.stratocloud.provider.huawei.nic.requirements;

import com.huaweicloud.sdk.vpc.v2.model.Port;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.nic.HuaweiNicHandler;
import com.stratocloud.provider.huawei.securitygroup.HuaweiSecurityGroupHandler;
import com.stratocloud.provider.relationship.EssentialRequirementHandler;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.ExternalRequirement;
import com.stratocloud.resource.ExternalResource;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiNicToSecurityGroupHandler implements EssentialRequirementHandler {

    private final HuaweiNicHandler nicHandler;

    private final HuaweiSecurityGroupHandler securityGroupHandler;

    public HuaweiNicToSecurityGroupHandler(HuaweiNicHandler nicHandler,
                                           HuaweiSecurityGroupHandler securityGroupHandler) {
        this.nicHandler = nicHandler;
        this.securityGroupHandler = securityGroupHandler;
    }

    @Override
    public String getRelationshipTypeId() {
        return "HUAWEI_NIC_TO_SECURITY_GROUP_RELATIONSHIP";
    }

    @Override
    public String getRelationshipTypeName() {
        return "网卡与安全组";
    }

    @Override
    public ResourceHandler getSource() {
        return nicHandler;
    }

    @Override
    public ResourceHandler getTarget() {
        return securityGroupHandler;
    }

    @Override
    public String getCapabilityName() {
        return "网卡";
    }

    @Override
    public String getRequirementName() {
        return "安全组";
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
    public List<ExternalRequirement> describeExternalRequirements(ExternalAccount account, ExternalResource source) {
        Optional<Port> port = nicHandler.describePort(account, source.externalId());

        if(port.isEmpty())
            return List.of();

        List<String> securityGroups = port.get().getSecurityGroups();

        if(Utils.isEmpty(securityGroups))
            return List.of();

        List<ExternalRequirement> result = new ArrayList<>();

        for (String securityGroupId : securityGroups) {
            var securityGroup = securityGroupHandler.describeExternalResource(account, securityGroupId);
            if(securityGroup.isEmpty())
                continue;
            result.add(new ExternalRequirement(
                    getRelationshipTypeId(),
                    securityGroup.get(),
                    Map.of()
            ));
        }

        return result;
    }
}
