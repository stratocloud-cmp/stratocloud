package com.stratocloud.provider.huawei.elb.zone;

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
public class HuaweiElbZoneSetHandler extends AbstractResourceHandler {

    private final HuaweiCloudProvider provider;

    public HuaweiElbZoneSetHandler(HuaweiCloudProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "HUAWEI_ELB_ZONE_SET";
    }

    @Override
    public String getResourceTypeName() {
        return "华为云ELB可用区集合";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.ZONE;
    }

    @Override
    public boolean isInfrastructure() {
        return true;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return describeZoneSet(account, externalId).map(
                set -> toExternalResource(account, set)
        );
    }

    public ExternalResource toExternalResource(ExternalAccount account, HuaweiElbZoneSet zoneSet) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                zoneSet.getZoneSetId(),
                zoneSet.getZoneSetName(),
                ResourceState.AVAILABLE
        );
    }

    public Optional<HuaweiElbZoneSet> describeZoneSet(ExternalAccount account, String externalId){
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).elb().describeZoneSet(externalId);
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        return provider.buildClient(account).elb().describeZoneSets().stream().map(
                set -> toExternalResource(account, set)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        ExternalResource zoneSet = describeExternalResource(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("ELB zone set not found")
        );

        resource.updateByExternal(zoneSet);
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
