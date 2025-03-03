package com.stratocloud.provider.huawei.elb.policy;

import com.huaweicloud.sdk.elb.v3.model.L7Policy;
import com.huaweicloud.sdk.elb.v3.model.ListL7PoliciesRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiElbPolicyHandler extends AbstractResourceHandler {

    private final HuaweiCloudProvider provider;

    public HuaweiElbPolicyHandler(HuaweiCloudProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "HUAWEI_ELB_POLICY";
    }

    @Override
    public String getResourceTypeName() {
        return "华为云ELB转发策略";
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
        return describePolicy(account, externalId).map(
                l7Policy -> toExternalResource(account, l7Policy)
        );
    }

    private ExternalResource toExternalResource(ExternalAccount account, L7Policy l7Policy) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                l7Policy.getId(),
                l7Policy.getName(),
                ResourceState.AVAILABLE
        );
    }

    public Optional<L7Policy> describePolicy(ExternalAccount account, String externalId){
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).elb().describePolicy(externalId);
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        return provider.buildClient(account).elb().describePolicies(new ListL7PoliciesRequest()).stream().map(
                l7Policy -> toExternalResource(account, l7Policy)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        ExternalResource policy = describeExternalResource(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("L7 policy not found")
        );

        resource.updateByExternal(policy);
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
