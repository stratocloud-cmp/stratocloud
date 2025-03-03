package com.stratocloud.provider.aliyun.securitygroup.policy;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.aliyun.securitygroup.AliyunSecurityGroup;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.resource.*;
import com.stratocloud.tag.Tag;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AliyunEgressPolicyHandler extends AbstractResourceHandler {

    public static final String DEST_GROUP_REL_TYPE = "ALIYUN_EGRESS_POLICY_TO_DEST_GROUP_RELATIONSHIP";
    public static final String SOURCE_GROUP_REL_TYPE = "ALIYUN_EGRESS_POLICY_TO_SOURCE_GROUP_RELATIONSHIP";
    private final AliyunCloudProvider provider;


    public AliyunEgressPolicyHandler(AliyunCloudProvider provider) {
        this.provider = provider;
    }


    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "ALIYUN_SECURITY_GROUP_EGRESS_POLICY";
    }

    @Override
    public String getResourceTypeName() {
        return "阿里云出站规则";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.SECURITY_GROUP_EGRESS_POLICY;
    }

    @Override
    public boolean isInfrastructure() {
        return true;
    }

    @Override
    public boolean isSharedRequirementTarget() {
        return false;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return describeEgressPolicy(account, externalId).map(
                securityGroupPolicy -> toExternalResource(account, securityGroupPolicy)
        );
    }

    public Optional<AliyunSecurityGroupPolicy> describeEgressPolicy(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        AliyunSecurityGroupPolicyId policyId = AliyunSecurityGroupPolicyId.fromString(externalId);
        return provider.buildClient(account).ecs().describeSecurityGroupPolicy(policyId);
    }

    private ExternalResource toExternalResource(ExternalAccount account,
                                                AliyunSecurityGroupPolicy securityGroupPolicy) {
        String securityGroupId = securityGroupPolicy.policyId().securityGroupId();
        AliyunSecurityGroup securityGroup = provider.buildClient(account).ecs().describeSecurityGroup(
                securityGroupId
        ).orElseThrow(() -> new ExternalResourceNotFoundException("Security group not found: "+securityGroupId));

        String externalId = securityGroupPolicy.policyId().toString();

        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                externalId,
                "%s-出站规则-%s".formatted(
                        securityGroup.detail().getSecurityGroupName(),
                        securityGroupPolicy.detail().getSecurityGroupRuleId()
                ),
                ResourceState.IN_USE
        );
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        AliyunClient client = provider.buildClient(account);
        return client.ecs().describeSecurityGroupPolicies().stream().filter(
                AliyunSecurityGroupPolicy::isEgress
        ).map(
                securityGroupPolicy -> toExternalResource(account, securityGroupPolicy)
        ).toList();
    }


    @Override
    public List<Tag> describeExternalTags(ExternalAccount account, ExternalResource externalResource) {
        return List.of();
    }

    @Override
    public void synchronize(Resource resource) {
        if(Utils.isBlank(resource.getExternalId())) {
            return;
        }

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        AliyunSecurityGroupPolicy policy = describeEgressPolicy(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("SecurityGroupPolicy not found: " + resource.getName())
        );

        resource.updateByExternal(toExternalResource(account, policy));

        RuntimeProperty cidrRuntimeProperty = RuntimeProperty.ofDisplayInList(
                "cidrBlock",
                "目标地址(IPv4)",
                policy.detail().getDestCidrIp(),
                policy.detail().getDestCidrIp()
        );

        RuntimeProperty ipv6CidrRuntimeProperty = RuntimeProperty.ofDisplayInList(
                "ipv6CidrBlock",
                "目标地址(IPv6)",
                policy.detail().getIpv6DestCidrIp(),
                policy.detail().getIpv6DestCidrIp()
        );

        RuntimeProperty actionRuntimeProperty = RuntimeProperty.ofDisplayInList(
                "action", "策略", policy.detail().getPolicy(), policy.detail().getPolicy()
        );

        String protocolAndPort = "%s:%s".formatted(policy.detail().getIpProtocol(), policy.detail().getPortRange());
        RuntimeProperty protocolAndPortRuntimeProperty = RuntimeProperty.ofDisplayInList(
                "protocolAndPort", "协议端口", protocolAndPort, protocolAndPort
        );

        resource.addOrUpdateRuntimeProperty(cidrRuntimeProperty);
        resource.addOrUpdateRuntimeProperty(ipv6CidrRuntimeProperty);
        resource.addOrUpdateRuntimeProperty(actionRuntimeProperty);
        resource.addOrUpdateRuntimeProperty(protocolAndPortRuntimeProperty);
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
