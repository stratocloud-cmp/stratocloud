package com.stratocloud.provider.huawei.vpc;

import com.huaweicloud.sdk.vpc.v2.model.Vpc;
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
public class HuaweiVpcHandler extends AbstractResourceHandler {

    private final HuaweiCloudProvider provider;

    public HuaweiVpcHandler(HuaweiCloudProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "HUAWEI_VPC";
    }

    @Override
    public String getResourceTypeName() {
        return "华为云VPC";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.VPC;
    }

    @Override
    public boolean isInfrastructure() {
        return true;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        Optional<Vpc> vpc = describeVpc(account, externalId);
        return vpc.map(
                v->toExternalResource(account, v)
        );
    }

    private ExternalResource toExternalResource(ExternalAccount account, Vpc vpc) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                vpc.getId(),
                vpc.getName(),
                ResourceState.AVAILABLE
        );
    }

    public Optional<Vpc> describeVpc(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).vpc().describeVpc(externalId);
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        return provider.buildClient(account).vpc().describeVpcs().stream().map(
                v -> toExternalResource(account,v)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Vpc vpc = describeVpc(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Vpc not found")
        );

        String cidr = vpc.getCidr();

        RuntimeProperty cidrProp = RuntimeProperty.ofDisplayInList(
                "cidr", "CIDR", cidr, cidr
        );

        resource.addOrUpdateRuntimeProperty(cidrProp);

        resource.updateByExternal(toExternalResource(account, vpc));
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }

    @Override
    public boolean isSharedRequirementTarget() {
        return true;
    }
}
