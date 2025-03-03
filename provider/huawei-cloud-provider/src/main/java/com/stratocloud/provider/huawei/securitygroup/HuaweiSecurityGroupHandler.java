package com.stratocloud.provider.huawei.securitygroup;

import com.huaweicloud.sdk.vpc.v2.model.ListSecurityGroupsRequest;
import com.huaweicloud.sdk.vpc.v2.model.SecurityGroup;
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
public class HuaweiSecurityGroupHandler extends AbstractResourceHandler {

    private final HuaweiCloudProvider provider;

    public HuaweiSecurityGroupHandler(HuaweiCloudProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "HUAWEI_SECURITY_GROUP";
    }

    @Override
    public String getResourceTypeName() {
        return "华为云安全组";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.SECURITY_GROUP;
    }

    @Override
    public boolean isInfrastructure() {
        return true;
    }

    @Override
    public boolean supportCascadedDestruction() {
        return true;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return describeSecurityGroup(account, externalId).map(
                s -> toExternalResource(account, s)
        );
    }

    private ExternalResource toExternalResource(ExternalAccount account, SecurityGroup securityGroup) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                securityGroup.getId(),
                securityGroup.getName(),
                ResourceState.AVAILABLE
        );
    }

    public Optional<SecurityGroup> describeSecurityGroup(ExternalAccount account, String externalId){
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).vpc().describeSecurityGroup(externalId);
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        return provider.buildClient(account).vpc().describeSecurityGroups(
                new ListSecurityGroupsRequest()
        ).stream().map(s -> toExternalResource(account, s)).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        SecurityGroup securityGroup = describeSecurityGroup(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Security group not found.")
        );

        resource.updateByExternal(toExternalResource(account, securityGroup));
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }


}
