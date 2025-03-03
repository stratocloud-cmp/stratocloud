package com.stratocloud.provider.aliyun.zone;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.aliyun.common.AliyunRegion;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.constants.TagEntries;
import com.stratocloud.resource.*;
import com.stratocloud.tag.Tag;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AliyunZoneHandler extends AbstractResourceHandler {

    private final AliyunCloudProvider provider;

    public AliyunZoneHandler(AliyunCloudProvider provider) {
        this.provider = provider;
    }


    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "ALIYUN_ZONE";
    }

    @Override
    public String getResourceTypeName() {
        return "阿里云可用区";
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
        var zoneInfo = describeZone(account, externalId);

        return zoneInfo.map(zone -> toExternalResource(account, zone));
    }

    private Optional<AliyunZone> describeZone(ExternalAccount account,
                                                                                                String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        AliyunClient client = provider.buildClient(account);
        return client.ecs().describeZone(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account,
                                                AliyunZone zone) {
        ResourceState zoneState = ResourceState.AVAILABLE;

        if("SoldOut".equals(zone.availability().getStatus()))
            zoneState = ResourceState.SOLD_OUT;

        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                zone.getZoneId(),
                zone.zone().getLocalName(),
                zoneState
        );
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        AliyunClient client = provider.buildClient(account);
        return client.ecs().describeZones().stream().map(zone -> toExternalResource(account, zone)).toList();
    }


    @Override
    public List<Tag> describeExternalTags(ExternalAccount account, ExternalResource externalResource) {
        AliyunClient client = provider.buildClient(account);
        Optional<AliyunRegion> region = AliyunRegion.fromId(client.getRegionId());

        Tag regionTag = region.map(
                r -> new Tag(TagEntries.REGION, r.getId(), r.getName(), r.ordinal())
        ).orElseGet(
                () -> new Tag(TagEntries.REGION, client.getRegionId(), client.getRegionId(), 100)
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
