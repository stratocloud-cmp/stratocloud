package com.stratocloud.provider.huawei.image;

import com.huaweicloud.sdk.ims.v2.model.ImageInfo;
import com.huaweicloud.sdk.ims.v2.model.ListImagesRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.TagFactory;
import com.stratocloud.provider.constants.CpuArch;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.common.HuaweiCloudClient;
import com.stratocloud.resource.*;
import com.stratocloud.tag.Tag;
import com.stratocloud.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.huaweicloud.sdk.ims.v2.model.ImageInfo.StatusEnum.*;

@Component
public class HuaweiImageHandler extends AbstractResourceHandler {

    private final HuaweiCloudProvider provider;

    public HuaweiImageHandler(HuaweiCloudProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "HUAWEI_IMAGE";
    }

    @Override
    public String getResourceTypeName() {
        return "华为云镜像";
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
                i -> toExternalResource(account, i)
        );
    }

    public Optional<ImageInfo> describeImage(ExternalAccount account, String externalId) {
        if(Utils.isBlank(externalId))
            return Optional.empty();

        return provider.buildClient(account).ims().describeImage(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account, ImageInfo image) {
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                image.getId(),
                image.getName(),
                convertStatus(image.getStatus())
        );
    }

    private ResourceState convertStatus(ImageInfo.StatusEnum status) {
        if(ACTIVE.equals(status))
            return ResourceState.AVAILABLE;
        else if(KILLED.equals(status))
            return ResourceState.BUILD_ERROR;
        else if(DELETED.equals(status))
            return ResourceState.DESTROYED;
        else if(SAVING.equals(status))
            return ResourceState.BUILDING;
        else if(QUEUED.equals(status))
            return ResourceState.UNAVAILABLE;
        else
            return ResourceState.UNKNOWN;
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        HuaweiCloudClient client = provider.buildClient(account);

        ListImagesRequest publicImagesRequest = getListPublicImagesRequest();
        List<ImageInfo> publicImages = client.ims().describeImages(publicImagesRequest);

        ListImagesRequest privateImagesRequest = getListPrivateImagesRequest(client.getProjectId());
        List<ImageInfo> privateImages = client.ims().describeImages(privateImagesRequest);

        List<ImageInfo> images = new ArrayList<>();
        images.addAll(publicImages);
        images.addAll(privateImages);

        return images.stream().map(
                i -> toExternalResource(account, i)
        ).toList();
    }

    private ListImagesRequest getListPublicImagesRequest(){
        return new ListImagesRequest()
                .withImagetype(ListImagesRequest.ImagetypeEnum.GOLD)
                .withVisibility(ListImagesRequest.VisibilityEnum.PUBLIC)
                .withProtected(true);
    }

    private ListImagesRequest getListPrivateImagesRequest(String projectId){
        return new ListImagesRequest()
                .withOwner(projectId);
    }

    @Override
    public List<Tag> describeExternalTags(ExternalAccount account, ExternalResource externalResource) {
        List<Tag> result = new ArrayList<>();

        Optional<ImageInfo> image = describeImage(account, externalResource.externalId());

        if(image.isEmpty())
            return result;

        ImageInfo.PlatformEnum platform = image.get().getPlatform();
        if(platform != null){
            Tag platformTag = TagFactory.buildImagePlatformTag(platform.getValue());
            result.add(platformTag);
        }

        CpuArch cpuArch = getCpuArch(image.get());
        Tag cpuArchTag = TagFactory.buildCpuArchTag(
                cpuArch
        );
        result.add(cpuArchTag);

        return result;
    }

    @NotNull
    private static CpuArch getCpuArch(ImageInfo image) {
        ImageInfo.OsBitEnum osBitEnum = image.getOsBit();
        boolean is32Bit = osBitEnum != null && Objects.equals(osBitEnum, ImageInfo.OsBitEnum._32);
        ImageInfo.SupportArmEnum supportArmEnum = image.getSupportArm();
        boolean supportArm = supportArmEnum != null && Objects.equals(supportArmEnum, ImageInfo.SupportArmEnum.TRUE);
        return supportArm ? CpuArch.ARM : (is32Bit ? CpuArch.X86_32 : CpuArch.X86_64);
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        ImageInfo image = describeImage(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Image not found.")
        );

        resource.updateByExternal(toExternalResource(account, image));

        String osVersion = image.getOsVersion();

        if(Utils.isNotBlank(osVersion)){
            RuntimeProperty osVersionProperty = RuntimeProperty.ofDisplayInList(
                    "osVersion", "操作系统", osVersion, osVersion
            );
            resource.addOrUpdateRuntimeProperty(osVersionProperty);
        }

        CpuArch cpuArch = getCpuArch(image);

        RuntimeProperty cpuArchProperty = RuntimeProperty.ofDisplayInList(
                "cpuArch", "芯片架构", cpuArch.name(), cpuArch.name()
        );
        resource.addOrUpdateRuntimeProperty(cpuArchProperty);


        if(image.getMinDisk() != null){
            String size = String.valueOf(image.getMinDisk());
            RuntimeProperty sizeProperty = RuntimeProperty.ofDisplayInList(
                    "size",
                    "镜像大小(GB)",
                    size,
                    size
            );
            resource.addOrUpdateRuntimeProperty(sizeProperty);
        }
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
