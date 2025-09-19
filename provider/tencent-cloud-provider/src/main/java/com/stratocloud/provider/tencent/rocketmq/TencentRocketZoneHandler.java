package com.stratocloud.provider.tencent.rocketmq;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.region.v20220627.models.ZoneInfo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentRocketZoneHandler extends AbstractResourceHandler {

    private final TencentCloudProvider provider;

    public TencentRocketZoneHandler(TencentCloudProvider provider) {
        this.provider = provider;
    }


    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "TENCENT_CLOUD_ROCKETMQ_ZONE";
    }

    @Override
    public String getResourceTypeName() {
        return "腾讯云RocketMQ可用区";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.MQ_ZONE;
    }

    @Override
    public boolean isInfrastructure() {
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
        return client.describeRocketZone(externalId);
    }

    public ExternalResource toExternalResource(ExternalAccount account, ZoneInfo zone) {
        ResourceState zoneState = ResourceState.AVAILABLE;

        if("UNAVAILABLE".equals(zone.getZoneState()))
            zoneState = ResourceState.UNAVAILABLE;

        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                zone.getZoneId(),
                zone.getZoneName(),
                zoneState
        );
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        TencentCloudClient client = provider.buildClient(account);
        return client.describeRocketZones().stream().map(zone -> toExternalResource(account, zone)).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        ExternalResource zone = describeExternalResource(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Zone not found: " + resource.getName())
        );

        resource.updateByExternal(zone);

        if(zone.state() == ResourceState.UNAVAILABLE)
            resource.markRecycled(false);
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
