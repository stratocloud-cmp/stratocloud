package com.stratocloud.provider.huawei.flavor;

import com.huaweicloud.sdk.ecs.v2.model.Flavor;
import com.huaweicloud.sdk.ecs.v2.model.FlavorExtraSpec;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.TagFactory;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.common.HuaweiCloudClient;
import com.stratocloud.resource.*;
import com.stratocloud.tag.Tag;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class HuaweiFlavorHandler extends AbstractResourceHandler {

    private final HuaweiCloudProvider provider;

    public HuaweiFlavorHandler(HuaweiCloudProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "HUAWEI_FLAVOR";
    }

    @Override
    public String getResourceTypeName() {
        return "华为云规格";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.FLAVOR;
    }

    @Override
    public boolean isInfrastructure() {
        return true;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return describeFlavor(account, externalId).map(
                flavor -> toExternalResource(account, flavor)
        );
    }

    public Optional<Flavor> describeFlavor(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).ecs().describeFlavor(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account, Flavor flavor) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                flavor.getId(),
                flavor.getName(),
                convertStatus(flavor)
        );
    }

    private ResourceState convertStatus(Flavor flavor) {
        Boolean isPublic = flavor.getOsFlavorAccessIsPublic();

        FlavorExtraSpec extraSpec = flavor.getOsExtraSpecs();

        boolean soldOut = isFlavorSoldOut(extraSpec);

        if (isPublic != null && isPublic && !soldOut)
            return ResourceState.AVAILABLE;

        return ResourceState.UNAVAILABLE;
    }

    public boolean isFlavorSoldOut(FlavorExtraSpec extraSpecs) {
        if(extraSpecs == null)
            return true;

        String status = extraSpecs.getCondOperationStatus();

        Map<String, Boolean> zoneStatusMap = getFlavorStatusInDifferentZones(extraSpecs.getCondOperationAz());

        if(Utils.isNotEmpty(zoneStatusMap)){
            for (var entry : zoneStatusMap.entrySet()) {
                if(entry.getValue())
                    return false;
            }
        }

        if(Utils.isBlank(status))
            return false;

        return !Set.of("normal", "promotion").contains(status);
    }

    public Map<String, Boolean> getFlavorStatusInDifferentZones(String condOperationAz){
        try {
            if(Utils.isBlank(condOperationAz))
                return Map.of();

            String[] azStatusList = condOperationAz.split(",");
            if(Utils.isEmpty(azStatusList))
                return Map.of();

            Map<String, Boolean> result = new HashMap<>();
            for (String azStatus : azStatusList) {
                if(Utils.isBlank(azStatus))
                    continue;

                int zoneEndIndex = azStatus.indexOf("(");
                int statusEndIndex = azStatus.indexOf(")");

                String zone = azStatus.substring(0, zoneEndIndex);
                String status = azStatus.substring(zoneEndIndex + 1, statusEndIndex);

                result.put(zone, Set.of("normal", "promotion").contains(status));
            }

            return result;
        }catch (Exception e){
            return Map.of();
        }
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        HuaweiCloudClient client = provider.buildClient(account);
        return client.ecs().describeFlavors().stream().map(
                flavor -> toExternalResource(account, flavor)
        ).toList();
    }

    @Override
    public List<Tag> describeExternalTags(ExternalAccount account, ExternalResource externalResource) {
        Optional<Flavor> flavor = describeFlavor(account, externalResource.externalId());

        List<Tag> result = new ArrayList<>();

        if(flavor.isEmpty())
            return result;


        Tag flavorSizeTag = TagFactory.buildFlavorSizeTag(
                Integer.parseInt(flavor.get().getVcpus()),
                flavor.get().getRam() >> 10
        );
        result.add(flavorSizeTag);

        FlavorExtraSpec extraSpec = flavor.get().getOsExtraSpecs();

        if(extraSpec != null && Utils.isNotBlank(extraSpec.getEcsGeneration())){
            String ecsGeneration = extraSpec.getEcsGeneration();
            Tag familyTag = TagFactory.buildFlavorFamilyTag(ecsGeneration, ecsGeneration, ecsGeneration.hashCode());
            result.add(familyTag);
        }

        return result;
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Flavor flavor = describeFlavor(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Flavor not found.")
        );

        resource.updateByExternal(toExternalResource(account, flavor));

        String sizeInfo = "%sC%sG".formatted(flavor.getVcpus(), flavor.getRam()>>10);
        RuntimeProperty sizeProperty = RuntimeProperty.ofDisplayInList(
                "size", "规格大小", sizeInfo, sizeInfo
        );

        resource.addOrUpdateRuntimeProperty(sizeProperty);
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
