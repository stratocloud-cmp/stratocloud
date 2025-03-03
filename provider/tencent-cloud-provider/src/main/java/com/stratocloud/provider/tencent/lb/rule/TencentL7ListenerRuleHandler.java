package com.stratocloud.provider.tencent.lb.rule;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentL7ListenerRuleHandler extends AbstractResourceHandler {

    private final TencentCloudProvider provider;

    public TencentL7ListenerRuleHandler(TencentCloudProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "TENCENT_L7_LISTENER_RULE";
    }

    @Override
    public String getResourceTypeName() {
        return "腾讯云七层转发规则";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.LOAD_BALANCER_RULE;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        Optional<TencentL7Rule> rule = describeRule(account, externalId);
        return rule.map(r -> toExternalResource(account, r));
    }

    private Optional<TencentL7Rule> describeRule(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();
        return provider.buildClient(account).describeL7Rule(
                TencentL7RuleId.fromString(externalId)
        );
    }

    public ExternalResource toExternalResource(ExternalAccount account, TencentL7Rule rule) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                rule.ruleId().toString(),
                rule.rule().getUrl(),
                ResourceState.IN_USE
        );
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        List<TencentL7Rule> rules = provider.buildClient(account).describeL7Rules();
        return rules.stream().map(
                rule -> toExternalResource(account, rule)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        TencentL7Rule rule = describeRule(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("LB rule not found.")
        );

        resource.updateByExternal(toExternalResource(account, rule));

        String domains = String.join(",", rule.rule().getDomains());
        RuntimeProperty domainProperty = RuntimeProperty.ofDisplayInList(
                "domain", "域名", domains, domains
        );
        resource.addOrUpdateRuntimeProperty(domainProperty);

        RuntimeProperty urlProperty = RuntimeProperty.ofDisplayInList(
                "url", "转发路径", rule.rule().getUrl(), rule.rule().getUrl()
        );
        resource.addOrUpdateRuntimeProperty(urlProperty);
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }

    @Override
    public boolean supportCascadedDestruction() {
        return true;
    }
}
