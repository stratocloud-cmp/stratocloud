package com.stratocloud.provider.huawei.zone;

import com.huaweicloud.sdk.ecs.v2.model.NovaAvailabilityZone;
import com.huaweicloud.sdk.ecs.v2.model.NovaAvailabilityZoneState;
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
public class HuaweiZoneHandler extends AbstractResourceHandler {

    private final HuaweiCloudProvider provider;

    public HuaweiZoneHandler(HuaweiCloudProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "HUAWEI_ZONE";
    }

    @Override
    public String getResourceTypeName() {
        return "华为云可用区";
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
        Optional<NovaAvailabilityZone> zone = describeZone(account, externalId);
        return zone.map(
                z->toExternalResource(account, z)
        );
    }

    private ExternalResource toExternalResource(ExternalAccount account, NovaAvailabilityZone zone) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                zone.getZoneName(),
                zone.getZoneName(),
                convertState(zone.getZoneState())
        );
    }

    private ResourceState convertState(NovaAvailabilityZoneState zoneState) {
        if(zoneState.getAvailable() != null && zoneState.getAvailable())
            return ResourceState.AVAILABLE;
        return ResourceState.UNAVAILABLE;
    }

    public Optional<NovaAvailabilityZone> describeZone(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).ecs().describeZone(externalId);
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        return provider.buildClient(account).ecs().describeZones().stream().map(
                z -> toExternalResource(account,z)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        ExternalResource zone = describeExternalResource(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Zone not found")
        );

        resource.updateByExternal(zone);
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
