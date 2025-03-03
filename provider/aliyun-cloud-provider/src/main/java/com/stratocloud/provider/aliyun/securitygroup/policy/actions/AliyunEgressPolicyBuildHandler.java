package com.stratocloud.provider.aliyun.securitygroup.policy.actions;

import com.aliyun.ecs20140526.models.AuthorizeSecurityGroupEgressRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.ip.InternetProtocol;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.securitygroup.policy.AliyunEgressPolicyHandler;
import com.stratocloud.provider.aliyun.securitygroup.policy.AliyunSecurityGroupPolicyId;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;


@Component
public class AliyunEgressPolicyBuildHandler implements BuildResourceActionHandler {

    private final AliyunEgressPolicyHandler policyHandler;

    public AliyunEgressPolicyBuildHandler(AliyunEgressPolicyHandler policyHandler) {
        this.policyHandler = policyHandler;
    }



    @Override
    public ResourceHandler getResourceHandler() {
        return policyHandler;
    }

    @Override
    public String getTaskName() {
        return "创建出站规则";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return AliyunEgressPolicyBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        Resource sourceGroup = resource.getEssentialTargetByType(AliyunEgressPolicyHandler.SOURCE_GROUP_REL_TYPE).orElseThrow(
                () -> new StratoException("Source group not found when adding policy.")
        );

        Optional<Resource> destGroup = resource.getExclusiveTargetByType(AliyunEgressPolicyHandler.DEST_GROUP_REL_TYPE);

        AliyunEgressPolicyBuildInput input = JSON.convert(parameters, AliyunEgressPolicyBuildInput.class);

        var request = new AuthorizeSecurityGroupEgressRequest();

        var permissions = new AuthorizeSecurityGroupEgressRequest.AuthorizeSecurityGroupEgressRequestPermissions();

        permissions.setDescription(resource.getDescription());

        destGroup.ifPresent(value -> permissions.setDestGroupId(value.getExternalId()));

        InternetProtocol internetProtocol = input.getInternetProtocol();

        if(internetProtocol == InternetProtocol.IPv4) {
            permissions.setSourceCidrIp(input.getSourceCidrIp());
            permissions.setDestCidrIp(input.getDestCidrIp());
        }
        if(internetProtocol == InternetProtocol.IPv6) {
            permissions.setIpv6SourceCidrIp(input.getIpv6SourceCidrIp());
            permissions.setIpv6DestCidrIp(input.getIpv6DestCidrIp());
        }

        permissions.setPolicy(input.getPolicy());
        permissions.setIpProtocol(input.getProtocol());


        if(Set.of("TCP", "UDP").contains(input.getProtocol())) {
            permissions.setPortRange(input.getPortRange());
            if(Utils.isNotBlank(input.getSourcePortRange()))
                permissions.setSourcePortRange(input.getSourcePortRange());
        } else {
            permissions.setPortRange("-1/-1");
            permissions.setSourcePortRange("-1/-1");
        }

        if(input.getPriority() != null)
            permissions.setPriority(input.getPriority().toString());

        request.setSecurityGroupId(sourceGroup.getExternalId());
        request.setPermissions(List.of(permissions));

        AliyunCloudProvider provider = (AliyunCloudProvider) policyHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        String ruleId = provider.buildClient(account).ecs().addSecurityGroupPolicy(request);
        resource.setExternalId(
                new AliyunSecurityGroupPolicyId(
                        sourceGroup.getExternalId(),
                        ruleId
                ).toString()
        );
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }

    @Override
    public List<String> getLockExclusiveTargetRelTypeIds() {
        return List.of(AliyunEgressPolicyHandler.SOURCE_GROUP_REL_TYPE);
    }
}
