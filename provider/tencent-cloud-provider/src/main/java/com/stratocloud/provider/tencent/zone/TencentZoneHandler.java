package com.stratocloud.provider.tencent.zone;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.constants.TagEntries;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.common.TencentCloudRegion;
import com.stratocloud.resource.*;
import com.stratocloud.tag.Tag;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.cvm.v20170312.models.ZoneInfo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentZoneHandler extends AbstractResourceHandler {

    private final TencentCloudProvider provider;

    public TencentZoneHandler(TencentCloudProvider provider) {
        this.provider = provider;
    }


    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "TENCENT_CLOUD_ZONE";
    }

    @Override
    public String getResourceTypeName() {
        return "腾讯云可用区";
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
    public boolean isSharedRequirementTarget() {
        return true;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        Optional<ZoneInfo> zoneInfo = describeZone(account, externalId);

        return zoneInfo.map(zone -> toExternalResource(account, zone));
    }

    private Optional<ZoneInfo> describeZone(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        TencentCloudClient client = provider.buildClient(account);
        return client.describeZone(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account, ZoneInfo zone) {
        ResourceState zoneState = ResourceState.AVAILABLE;

        if("UNAVAILABLE".equals(zone.getZoneState()))
            zoneState = ResourceState.UNAVAILABLE;

        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                zone.getZone(),
                zone.getZoneName(),
                zoneState
        );
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        TencentCloudClient client = provider.buildClient(account);
        return client.describeZones().stream().map(zone -> toExternalResource(account, zone)).toList();
    }


    @Override
    public List<Tag> describeExternalTags(ExternalAccount account, ExternalResource externalResource) {
        TencentCloudClient client = provider.buildClient(account);
        Optional<TencentCloudRegion> region = TencentCloudRegion.fromId(client.getRegion());

        Tag regionTag = region.map(
                r -> new Tag(TagEntries.REGION, r.getId(), r.getName(), r.ordinal())
        ).orElseGet(
                () -> new Tag(TagEntries.REGION, client.getRegion(), client.getRegion(), 100)
        );

        return List.of(regionTag);
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        ExternalResource zone = describeExternalResource(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Zone not found: " + resource.getName())
        );

        resource.updateByExternal(zone);
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
