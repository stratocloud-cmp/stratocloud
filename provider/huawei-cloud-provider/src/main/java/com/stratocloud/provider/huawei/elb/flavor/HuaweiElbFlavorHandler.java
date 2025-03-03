package com.stratocloud.provider.huawei.elb.flavor;

import com.huaweicloud.sdk.elb.v3.model.Flavor;
import com.huaweicloud.sdk.elb.v3.model.ListFlavorsRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class HuaweiElbFlavorHandler extends AbstractResourceHandler {

    private final HuaweiCloudProvider provider;

    public HuaweiElbFlavorHandler(HuaweiCloudProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.LB_FLAVOR;
    }

    @Override
    public boolean isInfrastructure() {
        return true;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return describeFlavor(account, externalId).map(flv -> toExternalResource(account, flv));
    }

    protected abstract boolean filterFlavor(Flavor flavor);

    public Optional<Flavor> describeFlavor(ExternalAccount account, String externalId){
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).elb().describeFlavor(externalId).filter(this::filterFlavor);
    }

    public ExternalResource toExternalResource(ExternalAccount account, Flavor flavor){
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                flavor.getId(),
                flavor.getName(),
                ResourceState.AVAILABLE
        );
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        return provider.buildClient(account).elb().describeFlavors(
                new ListFlavorsRequest()
        ).stream().filter(this::filterFlavor).map(
                flv->toExternalResource(account,flv)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Flavor flavor = describeFlavor(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Flavor not found.")
        );
        resource.updateByExternal(toExternalResource(account, flavor));
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
