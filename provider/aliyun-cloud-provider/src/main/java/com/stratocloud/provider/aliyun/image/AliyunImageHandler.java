package com.stratocloud.provider.aliyun.image;

import com.aliyun.ecs20140526.models.DescribeImagesRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.TagFactory;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.constants.CpuArch;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.resource.*;
import com.stratocloud.tag.Tag;
import com.stratocloud.tag.TagEntry;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AliyunImageHandler extends AbstractResourceHandler {
    private final AliyunCloudProvider provider;

    public AliyunImageHandler(AliyunCloudProvider provider) {
        this.provider = provider;
    }


    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "ALIYUN_IMAGE";
    }

    @Override
    public String getResourceTypeName() {
        return "阿里云镜像";
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

    private Optional<AliyunImage> describeImage(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).ecs().describeImage(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account, AliyunImage image) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                image.detail().getImageId(),
                image.detail().getImageName(),
                ResourceState.AVAILABLE
        );
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        AliyunClient client = provider.buildClient(account);
        DescribeImagesRequest request = new DescribeImagesRequest();
        return client.ecs().describeImages(request).stream().map(
                image -> toExternalResource(account, image)
        ).toList();
    }


    @Override
    public List<Tag> describeExternalTags(ExternalAccount account, ExternalResource externalResource) {
        Optional<AliyunImage> image = describeImage(account, externalResource.externalId());

        if(image.isEmpty())
            return new ArrayList<>();

        List<Tag> result = new ArrayList<>();

        Tag platformTag = TagFactory.buildImagePlatformTag(image.get().detail().getPlatform());

        CpuArch cpuArch = convertCpuArch(image.get().detail().getArchitecture());

        Tag archTag = TagFactory.buildCpuArchTag(cpuArch);

        result.add(platformTag);
        result.add(archTag);

        var tags = image.get().detail().getTags();

        if(tags != null && Utils.isNotEmpty(tags.getTag())){
            List<Tag> externalTags = tags.getTag().stream().map(
                    t -> new Tag(new TagEntry(t.getTagKey(), t.getTagKey()), t.getTagValue(), t.getTagValue(), 0)
            ).toList();

            result.addAll(externalTags);
        }


        return result;
    }

    private static CpuArch convertCpuArch(String architecture) {
        return switch (architecture){
            case "x86_64" -> CpuArch.X86_64;
            case "i386" -> CpuArch.X86_32;
            case "arm64" -> CpuArch.ARM;
            default -> CpuArch.UNKNOWN;
        };
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        AliyunImage image = describeImage(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Image not found: " + resource.getName())
        );

        ExternalResource externalResource = toExternalResource(account, image);

        resource.updateByExternal(externalResource);

        RuntimeProperty platformProperty = RuntimeProperty.ofDisplayInList(
                "platform",
                "镜像平台",
                image.detail().getPlatform(),
                image.detail().getPlatform()
        );

        RuntimeProperty archProperty = RuntimeProperty.ofDisplayInList(
                "cpuArch",
                "芯片架构",
                convertCpuArch(image.detail().getArchitecture()).name(),
                convertCpuArch(image.detail().getArchitecture()).name()
        );

        RuntimeProperty sizeProperty = RuntimeProperty.ofDisplayInList(
                "size",
                "镜像大小(GB)",
                String.valueOf(image.detail().getSize()),
                String.valueOf(image.detail().getSize())
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
