package com.stratocloud.provider.huawei.obs;

import com.obs.services.model.BucketStorageInfo;
import com.obs.services.model.ObsBucket;
import com.obs.services.model.StorageClassEnum;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.common.HuaweiCloudClient;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class HuaweiBucketHandler extends AbstractResourceHandler {

    private final HuaweiCloudProvider provider;

    public HuaweiBucketHandler(HuaweiCloudProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "HUAWEI_OBS_BUCKET";
    }

    @Override
    public String getResourceTypeName() {
        return "华为云存储桶";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.BUCKET;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return describeBucket(account, externalId).map(
                b -> toExternalResource(account, b)
        );
    }

    public Optional<ObsBucket> describeBucket(ExternalAccount account, String externalId){
        if(Utils.isBlank(externalId))
            return Optional.empty();
        HuaweiCloudClient client = provider.buildClient(account);
        return client.obs().describeBucket(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account, ObsBucket bucket){
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                bucket.getBucketName(),
                bucket.getBucketName(),
                ResourceState.IN_USE
        );
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        HuaweiCloudClient client = provider.buildClient(account);
        return client.obs().describeBuckets().stream().map(
                b -> toExternalResource(account, b)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        ExternalResource bucket = describeExternalResource(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Bucket not found")
        );
        resource.updateByExternal(bucket);

        HuaweiCloudClient client = provider.buildClient(account);


        HuaweiBucketSpec bucketSpec = HuaweiBucketSpec.getSpec(
                client,
                resource.getExternalId(),
                HuaweiBucketSpec::new
        );

        StorageClassEnum storageClass = bucketSpec.getStorageClass();
        RuntimeProperty storageClassProperty = RuntimeProperty.ofDisplayInList(
                "storageClass",
                "存储类型",
                String.valueOf(storageClass),
                String.valueOf(storageClass)
        );
        resource.addOrUpdateRuntimeProperty(storageClassProperty);

        RuntimeProperty multiAzProperty = RuntimeProperty.ofDisplayInList(
                "multiAz",
                "容灾类型",
                String.valueOf(bucketSpec.isEnableMultiAz()),
                bucketSpec.isEnableMultiAz() ? "多AZ" : "单AZ"
        );
        resource.addOrUpdateRuntimeProperty(multiAzProperty);

        Optional<BucketStorageInfo> bucketStat = client.obs().describeBucketStat(bucket.externalId());

        if(bucketStat.isPresent()){
            long storageSize = bucketStat.get().getSize();
            long objectCount = bucketStat.get().getObjectNumber();

            String storageSizeStr = "%.2f".formatted(
                    storageSize / (double) (1 << 30)
            );
            RuntimeProperty storageSizeProperty = RuntimeProperty.ofDisplayInList(
                    "storageSize",
                    "存储量(GB)",
                    storageSizeStr,
                    storageSizeStr
            );
            resource.addOrUpdateRuntimeProperty(storageSizeProperty);

            String objectCountStr = String.valueOf(objectCount);
            RuntimeProperty objectCountProperty = RuntimeProperty.ofDisplayable(
                    "objectCount",
                    "Object总数",
                    objectCountStr,
                    objectCountStr
            );
            resource.addOrUpdateRuntimeProperty(objectCountProperty);
        }
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
