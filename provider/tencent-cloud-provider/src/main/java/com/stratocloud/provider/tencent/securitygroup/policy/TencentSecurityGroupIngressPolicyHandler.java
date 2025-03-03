package com.stratocloud.provider.tencent.securitygroup.policy;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.constants.SecurityGroupPolicyDirection;
import com.stratocloud.provider.resource.TransientResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.resource.*;
import com.stratocloud.tag.Tag;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.vpc.v20170312.models.SecurityGroup;
import com.tencentcloudapi.vpc.v20170312.models.SecurityGroupPolicy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentSecurityGroupIngressPolicyHandler
        extends AbstractResourceHandler implements TransientResourceHandler {

    private final TencentCloudProvider provider;


    public TencentSecurityGroupIngressPolicyHandler(TencentCloudProvider provider) {
        this.provider = provider;
    }


    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "TENCENT_CLOUD_SECURITY_GROUP_INGRESS_POLICY";
    }

    @Override
    public String getResourceTypeName() {
        return "腾讯云入站规则";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.SECURITY_GROUP_INGRESS_POLICY;
    }

    @Override
    public boolean isInfrastructure() {
        return true;
    }


    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return describeIngressPolicy(account, externalId).map(
                securityGroupPolicy -> toExternalResource(account, securityGroupPolicy)
        );
    }

    public Optional<SecurityGroupPolicy> describeIngressPolicy(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        TencentSecurityGroupPolicyId policyId = TencentSecurityGroupPolicyId.fromString(externalId);
        return provider.buildClient(account).describeSecurityGroupPolicy(policyId);
    }

    private ExternalResource toExternalResource(ExternalAccount account,
                                                SecurityGroupPolicy securityGroupPolicy) {
        String securityGroupId = securityGroupPolicy.getSecurityGroupId();
        SecurityGroup securityGroup = provider.buildClient(account).describeSecurityGroup(
                securityGroupId
        ).orElseThrow(() -> new ExternalResourceNotFoundException("Security group not found: "+securityGroupId));

        String externalId = TencentSecurityGroupPolicyId.fromPolicy(
                securityGroupId,
                SecurityGroupPolicyDirection.ingress,
                securityGroupPolicy
        ).toString();

        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                externalId,
                "%s-入站规则-%s".formatted(securityGroup.getSecurityGroupName(), securityGroupPolicy.getPolicyIndex()),
                ResourceState.IN_USE
        );
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        TencentCloudClient client = provider.buildClient(account);
        return client.describeSecurityGroupPolicies(SecurityGroupPolicyDirection.ingress).stream().map(
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

        SecurityGroupPolicy policy = describeIngressPolicy(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("SecurityGroupPolicy not found: " + resource.getName())
        );

        resource.updateByExternal(toExternalResource(account, policy));

        RuntimeProperty cidrRuntimeProperty = RuntimeProperty.ofDisplayInList(
                "cidrBlock", "源地址(IPv4)", policy.getCidrBlock(), policy.getCidrBlock()
        );

        RuntimeProperty ipv6CidrRuntimeProperty = RuntimeProperty.ofDisplayInList(
                "ipv6CidrBlock", "源地址(IPv6)", policy.getIpv6CidrBlock(), policy.getIpv6CidrBlock()
        );

        RuntimeProperty actionRuntimeProperty = RuntimeProperty.ofDisplayInList(
                "action", "策略", policy.getAction(), policy.getAction()
        );

        String protocolAndPort = "%s:%s".formatted(policy.getProtocol(), policy.getPort());
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
