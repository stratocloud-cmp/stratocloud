package com.stratocloud.provider.tencent.flavor;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.TagFactory;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.resource.*;
import com.stratocloud.tag.Tag;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.cvm.v20170312.models.*;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TencentFlavorHandler extends AbstractResourceHandler {
    private final TencentCloudProvider provider;

    public TencentFlavorHandler(TencentCloudProvider provider) {
        this.provider = provider;
    }


    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "TENCENT_CLOUD_FLAVOR";
    }

    @Override
    public String getResourceTypeName() {
        return "腾讯云机型规格";
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
    public boolean isSharedRequirementTarget() {
        return false;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return describeFlavor(account, externalId).map(
                flavor -> toExternalResource(
                        account,
                        flavor,
                        describeFlavorConfig(account, externalId).orElseThrow()
                )
        );
    }

    public Optional<InstanceTypeConfig> describeFlavor(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        TencentFlavorId flavorId = TencentFlavorId.fromString(externalId);

        return provider.buildClient(account).describeInstanceType(flavorId);
    }

    private Optional<InstanceTypeQuotaItem> describeFlavorConfig(ExternalAccount account, String externalId) {
        TencentFlavorId flavorId = TencentFlavorId.fromString(externalId);
        return provider.buildClient(account).describeInstanceTypeQuotaItem(flavorId);
    }

    private ExternalResource toExternalResource(ExternalAccount account,
                                                InstanceTypeConfig flavor,
                                                InstanceTypeQuotaItem quotaItem) {


        Optional<ZoneInfo> zoneInfo = provider.buildClient(account).describeZone(flavor.getZone());

        String externalId = new TencentFlavorId(flavor.getZone(), flavor.getInstanceType()).toString();

        String resourceName = zoneInfo.map(
                z -> "%s(%s)".formatted(flavor.getInstanceType(), z.getZoneName())
        ).orElseThrow(
                () -> new ExternalResourceNotFoundException("Zone %s not found.".formatted(flavor.getZone()))
        );

        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                externalId,
                resourceName,
                convertState(quotaItem != null ? quotaItem.getStatus() : "SOLD_OUT")
        );
    }

    private ResourceState convertState(String status) {
        return switch (status){
            case "SELL" -> ResourceState.AVAILABLE;
            case "SOLD_OUT" -> ResourceState.SOLD_OUT;
            default -> ResourceState.UNKNOWN;
        };
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        TencentCloudClient client = provider.buildClient(account);

        DescribeZoneInstanceConfigInfosRequest configRequest = new DescribeZoneInstanceConfigInfosRequest();
        List<InstanceTypeQuotaItem> quotaItems = client.describeInstanceTypeQuotaItems(configRequest);
        Map<TencentFlavorId, InstanceTypeQuotaItem> map = new HashMap<>();

        for (InstanceTypeQuotaItem quotaItem : quotaItems) {
            map.put(new TencentFlavorId(quotaItem.getZone(), quotaItem.getInstanceType()), quotaItem);
        }

        DescribeInstanceTypeConfigsRequest request = new DescribeInstanceTypeConfigsRequest();
        return client.describeInstanceTypes(request).stream().map(
                flavor -> toExternalResource(
                        account,
                        flavor,
                        map.get(new TencentFlavorId(flavor.getZone(), flavor.getInstanceType()))
                )
        ).toList();
    }

    private Pair<InstanceFamilyConfig, Integer> describeFamilyAndIndex(ExternalAccount account, String familyId){
        TencentCloudClient client = provider.buildClient(account);
        List<InstanceFamilyConfig> familyConfigs = client.describeInstanceFamilies();

        for (int i = 0; i < familyConfigs.size(); i++) {
            InstanceFamilyConfig familyConfig = familyConfigs.get(i);

            if(Objects.equals(familyId, familyConfig.getInstanceFamily()))
                return Pair.of(familyConfig, i);
        }

        throw new StratoException("Instance family %s not found.".formatted(familyId));
    }


    @Override
    public List<Tag> describeExternalTags(ExternalAccount account, ExternalResource externalResource) {
        Optional<InstanceTypeConfig> flavor = describeFlavor(account, externalResource.externalId());

        if(flavor.isEmpty())
            return List.of();

        Pair<InstanceFamilyConfig, Integer> pair = describeFamilyAndIndex(account, flavor.get().getInstanceFamily());
        InstanceFamilyConfig familyConfig = pair.getFirst();
        Integer index = pair.getSecond();

        Tag familyTag = TagFactory.buildFlavorFamilyTag(
                familyConfig.getInstanceFamily(),
                familyConfig.getInstanceFamilyName(),
                index
        );

        int cpuCores = flavor.get().getCPU() == null ? 0 : flavor.get().getCPU().intValue();
        int memoryGb = flavor.get().getMemory() == null ? 0 : flavor.get().getMemory().intValue();

        Tag sizeTag = TagFactory.buildFlavorSizeTag(cpuCores, memoryGb);

        return List.of(familyTag, sizeTag);
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        InstanceTypeConfig flavor = describeFlavor(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Flavor not found: "+resource.getExternalId())
        );

        InstanceTypeQuotaItem flavorConfig = describeFlavorConfig(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Flavor config not found: "+resource.getExternalId())
        );

        ExternalResource externalResource = toExternalResource(account, flavor, flavorConfig);

        resource.updateByExternal(externalResource);

        RuntimeProperty cpuProperty = RuntimeProperty.ofDisplayInList(
                "cpuCores",
                "CPU核数",
                String.valueOf(flavor.getCPU()),
                String.valueOf(flavor.getCPU())
        );

        RuntimeProperty cpuModelProperty = RuntimeProperty.ofDisplayInList(
                "cpuModel",
                "CPU型号",
                flavorConfig.getCpuType(),
                flavorConfig.getCpuType()
        );

        RuntimeProperty cpuFreqProperty = RuntimeProperty.ofDisplayInList(
                "cpuFreq",
                "CPU主频",
                flavorConfig.getFrequency(),
                flavorConfig.getFrequency()
        );

        RuntimeProperty memoryProperty = RuntimeProperty.ofDisplayInList(
                "memoryGb",
                "内存大小",
                String.valueOf(flavor.getMemory()),
                String.valueOf(flavor.getMemory())
        );

        RuntimeProperty gpuProperty = RuntimeProperty.ofDisplayInList(
                "gpuCores",
                "GPU核数",
                String.valueOf(flavor.getGPU()),
                String.valueOf(flavor.getGPU())
        );

        RuntimeProperty gpuCountProperty = RuntimeProperty.ofDisplayInList(
                "gpuCount",
                "GPU卡数",
                String.valueOf(flavor.getGpuCount()),
                String.valueOf(flavor.getGpuCount())
        );

        Float price = flavorConfig.getPrice().getDiscountPrice();

        RuntimeProperty priceProperty = RuntimeProperty.ofDisplayInList(
                "cost",
                "参考价格(每月)",
                price == null ? "-":(price+"元"),
                price == null ? "-":(price+"元")
        );

        resource.addOrUpdateRuntimeProperty(cpuProperty);
        resource.addOrUpdateRuntimeProperty(cpuModelProperty);
        resource.addOrUpdateRuntimeProperty(cpuFreqProperty);

        resource.addOrUpdateRuntimeProperty(memoryProperty);
        resource.addOrUpdateRuntimeProperty(gpuProperty);
        resource.addOrUpdateRuntimeProperty(gpuCountProperty);
        resource.addOrUpdateRuntimeProperty(priceProperty);
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
