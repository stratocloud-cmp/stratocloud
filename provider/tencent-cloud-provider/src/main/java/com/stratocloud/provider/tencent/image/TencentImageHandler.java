package com.stratocloud.provider.tencent.image;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.TagFactory;
import com.stratocloud.provider.constants.CpuArch;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.resource.*;
import com.stratocloud.tag.Tag;
import com.stratocloud.tag.TagEntry;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.cvm.v20170312.models.DescribeImagesRequest;
import com.tencentcloudapi.cvm.v20170312.models.Image;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TencentImageHandler extends AbstractResourceHandler {
    private final TencentCloudProvider provider;

    public TencentImageHandler(TencentCloudProvider provider) {
        this.provider = provider;
    }


    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "TENCENT_CLOUD_IMAGE";
    }

    @Override
    public String getResourceTypeName() {
        return "腾讯云镜像";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.IMAGE;
    }

    @Override
    public boolean isInfrastructure() {
        return true;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return describeImage(account, externalId).map(
                image -> toExternalResource(account, image)
        );
    }

    private Optional<Image> describeImage(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).describeImage(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account, Image image) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                image.getImageId(),
                image.getImageName(),
                ResourceState.AVAILABLE
        );
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        TencentCloudClient client = provider.buildClient(account);
        DescribeImagesRequest request = new DescribeImagesRequest();
        return client.describeImages(request).stream().map(
                image -> toExternalResource(account, image)
        ).toList();
    }


    @Override
    public List<Tag> describeExternalTags(ExternalAccount account, ExternalResource externalResource) {
        Optional<Image> image = describeImage(account, externalResource.externalId());

        if(image.isEmpty())
            return new ArrayList<>();

        List<Tag> result = new ArrayList<>();

        Tag platformTag = TagFactory.buildImagePlatformTag(image.get().getPlatform());

        CpuArch cpuArch = convertCpuArch(image.get().getArchitecture());

        Tag archTag = TagFactory.buildCpuArchTag(cpuArch);

        result.add(platformTag);
        result.add(archTag);

        if(image.get().getTags() != null){
            List<Tag> externalTags = Arrays.stream(image.get().getTags()).map(
                    t -> new Tag(new TagEntry(t.getKey(), t.getKey()), t.getValue(), t.getValue(), 0)
            ).toList();

            result.addAll(externalTags);
        }


        return result;
    }

    private static CpuArch convertCpuArch(String architecture) {
        return switch (architecture){
            case "x86_64" -> CpuArch.X86_64;
            case "x86_32" -> CpuArch.X86_32;
            case "arm" -> CpuArch.ARM;
            default -> CpuArch.UNKNOWN;
        };
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Image image = describeImage(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Image not found: " + resource.getName())
        );

        ExternalResource externalResource = toExternalResource(account, image);

        resource.updateByExternal(externalResource);

        RuntimeProperty platformProperty = RuntimeProperty.ofDisplayInList(
                "platform",
                "镜像平台",
                image.getPlatform(),
                image.getPlatform()
        );

        RuntimeProperty archProperty = RuntimeProperty.ofDisplayInList(
                "cpuArch",
                "芯片架构",
                convertCpuArch(image.getArchitecture()).name(),
                convertCpuArch(image.getArchitecture()).name()
        );

        RuntimeProperty sizeProperty = RuntimeProperty.ofDisplayInList(
                "size",
                "镜像大小(GB)",
                String.valueOf(image.getImageSize()),
                String.valueOf(image.getImageSize())
        );

        resource.addOrUpdateRuntimeProperty(platformProperty);
        resource.addOrUpdateRuntimeProperty(archProperty);
        resource.addOrUpdateRuntimeProperty(sizeProperty);
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
