package com.stratocloud.provider.huawei.securitygroup;

import com.huaweicloud.sdk.vpc.v2.model.ListSecurityGroupRulesRequest;
import com.huaweicloud.sdk.vpc.v2.model.SecurityGroup;
import com.huaweicloud.sdk.vpc.v2.model.SecurityGroupRule;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public abstract class HuaweiSecurityGroupRuleHandler extends AbstractResourceHandler {

    private final HuaweiCloudProvider provider;

    public HuaweiSecurityGroupRuleHandler(HuaweiCloudProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public boolean isInfrastructure() {
        return true;
    }

    protected abstract boolean filterRule(SecurityGroupRule rule);

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return describeSecurityGroupRule(account, externalId).map(
                rule -> toExternalResource(account, rule)
        );
    }

    private ExternalResource toExternalResource(ExternalAccount account, SecurityGroupRule rule) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                rule.getId(),
                rule.getId(),
                ResourceState.AVAILABLE
        );
    }

    public Optional<SecurityGroupRule> describeSecurityGroupRule(ExternalAccount account, String externalId){
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).vpc().describeSecurityGroupRule(externalId).filter(this::filterRule);
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        return provider.buildClient(account).vpc().describeSecurityGroupRules(
                new ListSecurityGroupRulesRequest()
        ).stream().filter(this::filterRule).map(
                rule -> toExternalResource(account, rule)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        SecurityGroupRule rule = describeSecurityGroupRule(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Security group rule not found")
        );

        resource.updateByExternal(toExternalResource(account, rule));

        if(Utils.isNotBlank(rule.getRemoteIpPrefix())){
            String keyName = Objects.equals(rule.getDirection(), "ingress") ? "源地址" : "目的地址";

            RuntimeProperty remoteIpProperty = RuntimeProperty.ofDisplayInList(
                    "remoteIpPrefix", keyName, rule.getRemoteIpPrefix(), rule.getRemoteIpPrefix()
            );
            resource.addOrUpdateRuntimeProperty(remoteIpProperty);
        }

        if(Utils.isNotBlank(rule.getRemoteGroupId()) &&
                !Objects.equals(rule.getRemoteGroupId(), rule.getSecurityGroupId())){
            Optional<SecurityGroup> remoteGroup
                    = provider.buildClient(account).vpc().describeSecurityGroup(rule.getRemoteGroupId());

            String keyName = Objects.equals(rule.getDirection(), "ingress") ? "源安全组" : "目的安全组";

            RuntimeProperty remoteGroupProperty = RuntimeProperty.ofDisplayInList(
                    "remoteGroup",
                    keyName,
                    rule.getRemoteGroupId(),
                    remoteGroup.isPresent() ? remoteGroup.get().getName() : rule.getRemoteGroupId()
            );
            resource.addOrUpdateRuntimeProperty(remoteGroupProperty);
        }

        String protocol = Utils.isBlank(rule.getProtocol()) ? "all" : rule.getProtocol();
        RuntimeProperty protocolProperty = RuntimeProperty.ofDisplayInList(
                "protocol",
                "协议",
                protocol,
                protocol
        );
        resource.addOrUpdateRuntimeProperty(protocolProperty);

        Integer portMax = rule.getPortRangeMax() != null ? rule.getPortRangeMax() : 65535;
        Integer portMin = rule.getPortRangeMin() != null ? rule.getPortRangeMin() : 1;

        RuntimeProperty portProperty = RuntimeProperty.ofDisplayInList(
                "port",
                "端口",
                "%s-%s".formatted(portMin, portMax),
                "%s-%s".formatted(portMin, portMax)
        );
        resource.addOrUpdateRuntimeProperty(portProperty);
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
