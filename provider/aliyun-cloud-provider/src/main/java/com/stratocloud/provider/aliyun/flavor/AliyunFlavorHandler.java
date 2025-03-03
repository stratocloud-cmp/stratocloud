package com.stratocloud.provider.aliyun.flavor;

import com.aliyun.ecs20140526.models.DescribeInstanceTypesRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.TagFactory;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.aliyun.zone.AliyunZone;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.resource.*;
import com.stratocloud.tag.Tag;
import com.stratocloud.utils.Utils;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class AliyunFlavorHandler extends AbstractResourceHandler {
    private final AliyunCloudProvider provider;

    public AliyunFlavorHandler(AliyunCloudProvider provider) {
        this.provider = provider;
    }


    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "ALIYUN_FLAVOR";
    }

    @Override
    public String getResourceTypeName() {
        return "阿里云机型规格";
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
                        flavor
                )
        );
    }

    public Optional<AliyunFlavor> describeFlavor(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        AliyunFlavorId flavorId = AliyunFlavorId.fromString(externalId);

        return provider.buildClient(account).ecs().describeFlavor(flavorId);
    }

    private ExternalResource toExternalResource(ExternalAccount account,
                                                AliyunFlavor flavor) {
        Optional<AliyunZone> zoneInfo = provider.buildClient(account).ecs().describeZone(flavor.flavorId().zoneId());

        String resourceName = zoneInfo.map(
                z -> "%s(%s)".formatted(flavor.detail().getInstanceTypeId(), z.zone().getLocalName())
        ).orElseThrow(
                () -> new ExternalResourceNotFoundException("Zone %s not found.".formatted(flavor.flavorId().zoneId()))
        );

        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                flavor.flavorId().toString(),
                resourceName,
                flavor.state()
        );
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        AliyunClient client = provider.buildClient(account);
        DescribeInstanceTypesRequest request = new DescribeInstanceTypesRequest();
        return client.ecs().describeFlavors(request).stream().map(
                flavor -> toExternalResource(account, flavor)
        ).toList();
    }

    private Pair<AliyunFlavorFamily, Integer> describeFamilyAndIndex(ExternalAccount account, String familyId){
        AliyunClient client = provider.buildClient(account);
        List<AliyunFlavorFamily> families = client.ecs().describeInstanceFamilies();

        for (int i = 0; i < families.size(); i++) {
            AliyunFlavorFamily family = families.get(i);

            if(Objects.equals(familyId, family.detail().getInstanceTypeFamilyId()))
                return Pair.of(family, i);
        }

        throw new StratoException("Instance family %s not found.".formatted(familyId));
    }


    @Override
    public List<Tag> describeExternalTags(ExternalAccount account, ExternalResource externalResource) {
        Optional<AliyunFlavor> flavor = describeFlavor(account, externalResource.externalId());

        if(flavor.isEmpty())
            return List.of();

        String familyId = flavor.get().detail().getInstanceTypeFamily();

        Pair<AliyunFlavorFamily, Integer> pair = describeFamilyAndIndex(account, familyId);
        AliyunFlavorFamily family = pair.getFirst();
        Integer index = pair.getSecond();

        Tag familyTag = TagFactory.buildFlavorFamilyTag(
                family.detail().getInstanceTypeFamilyId(),
                family.detail().getInstanceTypeFamilyId(),
                index
        );

        int cpuCores = flavor.get().detail().getCpuCoreCount() == null ? 0 : flavor.get().detail().getCpuCoreCount();
        Float memoryGb = flavor.get().detail().getMemorySize() == null ? 0.0f : flavor.get().detail().getMemorySize();

        Tag sizeTag = TagFactory.buildFlavorSizeTag(cpuCores, memoryGb);

        return List.of(familyTag, sizeTag);
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        AliyunFlavor flavor = describeFlavor(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Flavor not found: "+resource.getExternalId())
        );

        ExternalResource externalResource = toExternalResource(account, flavor);

        resource.updateByExternal(externalResource);

        RuntimeProperty cpuProperty = RuntimeProperty.ofDisplayInList(
                "cpuCores",
                "CPU核数",
                String.valueOf(flavor.detail().getCpuCoreCount()),
                String.valueOf(flavor.detail().getCpuCoreCount())
        );

        RuntimeProperty cpuModelProperty = RuntimeProperty.ofDisplayInList(
                "cpuModel",
                "CPU型号",
                flavor.detail().getPhysicalProcessorModel(),
                flavor.detail().getPhysicalProcessorModel()
        );

        RuntimeProperty cpuFreqProperty = RuntimeProperty.ofDisplayInList(
                "cpuFreq",
                "CPU主频",
                String.valueOf(flavor.detail().getCpuSpeedFrequency()),
                String.valueOf(flavor.detail().getCpuSpeedFrequency())
        );

        resource.addOrUpdateRuntimeProperty(cpuProperty);
        resource.addOrUpdateRuntimeProperty(cpuModelProperty);
        resource.addOrUpdateRuntimeProperty(cpuFreqProperty);

        RuntimeProperty memoryProperty = RuntimeProperty.ofDisplayInList(
                "memoryGb",
                "内存大小(GB)",
                String.valueOf(flavor.detail().getMemorySize()),
                String.valueOf(flavor.detail().getMemorySize())
        );
        resource.addOrUpdateRuntimeProperty(memoryProperty);


        RuntimeProperty gpuCountProperty = RuntimeProperty.ofDisplayInList(
                "gpuCount",
                "GPU卡数",
                String.valueOf(flavor.detail().getGPUAmount()),
                String.valueOf(flavor.detail().getGPUAmount())
        );
        resource.addOrUpdateRuntimeProperty(gpuCountProperty);


        if(Utils.isNotBlank(flavor.detail().getGPUSpec())){
            RuntimeProperty gpuSpecProperty = RuntimeProperty.ofDisplayInList(
                    "gpuSpec",
                    "GPU型号",
                    String.valueOf(flavor.detail().getGPUSpec()),
                    String.valueOf(flavor.detail().getGPUSpec())
            );
            resource.addOrUpdateRuntimeProperty(gpuSpecProperty);
        }

        if(flavor.detail().getGPUMemorySize() != null){
            RuntimeProperty gpuMemoryProperty = RuntimeProperty.ofDisplayInList(
                    "gpiMemory",
                    "GPU显存(GB)",
                    String.valueOf(flavor.detail().getGPUMemorySize()),
                    String.valueOf(flavor.detail().getGPUMemorySize())
            );
            resource.addOrUpdateRuntimeProperty(gpuMemoryProperty);
        }
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
