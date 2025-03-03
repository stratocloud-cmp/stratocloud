package com.stratocloud.provider.aliyun.securitygroup.policy.actions;

import com.aliyun.ecs20140526.models.AuthorizeSecurityGroupRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.ip.InternetProtocol;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.securitygroup.policy.AliyunIngressPolicyHandler;
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
public class AliyunIngressPolicyBuildHandler implements BuildResourceActionHandler {

    private final AliyunIngressPolicyHandler policyHandler;

    public AliyunIngressPolicyBuildHandler(AliyunIngressPolicyHandler policyHandler) {
        this.policyHandler = policyHandler;
    }


    @Override
    public ResourceHandler getResourceHandler() {
        return policyHandler;
    }

    @Override
    public String getTaskName() {
        return "创建入站规则";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return AliyunIngressPolicyBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        Resource destGroup = resource.getEssentialTargetByType(AliyunIngressPolicyHandler.DEST_GROUP_REL_TYPE).orElseThrow(
                () -> new StratoException("Dest group not found when adding policy.")
        );

        Optional<Resource> sourceGroup = resource.getExclusiveTargetByType(AliyunIngressPolicyHandler.SOURCE_GROUP_REL_TYPE);

        AliyunIngressPolicyBuildInput input = JSON.convert(parameters, AliyunIngressPolicyBuildInput.class);

        var request = new AuthorizeSecurityGroupRequest();

        var permissions = new AuthorizeSecurityGroupRequest.AuthorizeSecurityGroupRequestPermissions();

        permissions.setDescription(resource.getDescription());

        InternetProtocol internetProtocol = input.getInternetProtocol();

        if(internetProtocol == InternetProtocol.IPv4) {
            permissions.setDestCidrIp(input.getDestCidrIp());
            permissions.setSourceCidrIp(input.getSourceCidrIp());
        }

        if(internetProtocol == InternetProtocol.IPv6) {
            permissions.setIpv6DestCidrIp(input.getIpv6DestCidrIp());
            permissions.setIpv6SourceCidrIp(input.getIpv6SourceCidrIp());
        }


        sourceGroup.ifPresent(value -> permissions.setSourceGroupId(value.getExternalId()));

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

        request.setSecurityGroupId(destGroup.getExternalId());
        request.setPermissions(List.of(permissions));

        AliyunCloudProvider provider = (AliyunCloudProvider) policyHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        String ruleId = provider.buildClient(account).ecs().addSecurityGroupPolicy(request);
        resource.setExternalId(
                new AliyunSecurityGroupPolicyId(
                        destGroup.getExternalId(),
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
        return List.of(AliyunIngressPolicyHandler.DEST_GROUP_REL_TYPE);
    }
}
