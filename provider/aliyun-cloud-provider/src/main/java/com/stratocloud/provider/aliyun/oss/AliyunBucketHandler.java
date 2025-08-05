package com.stratocloud.provider.aliyun.oss;

import com.aliyun.oss.model.Bucket;
import com.aliyun.oss.model.DataRedundancyType;
import com.aliyun.oss.model.StorageClass;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AliyunBucketHandler extends AbstractResourceHandler {

    private final AliyunCloudProvider provider;

    public AliyunBucketHandler(AliyunCloudProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "ALIYUN_OSS_BUCKET";
    }

    @Override
    public String getResourceTypeName() {
        return "阿里云存储桶";
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

    public Optional<Bucket> describeBucket(ExternalAccount account, String externalId){
        if(Utils.isBlank(externalId))
            return Optional.empty();
        AliyunClient client = provider.buildClient(account);
        return client.oss().describeBucket(externalId);
    }

    private ExternalResource toExternalResource(ExternalAccount account, Bucket bucket){
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                bucket.getName(),
                bucket.getName(),
                ResourceState.IN_USE
        );
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        AliyunClient client = provider.buildClient(account);
        return client.oss().describeBuckets().stream().map(
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

        AliyunClient client = provider.buildClient(account);


        AliyunBucketSpec bucketSpec = AliyunBucketSpec.getSpec(
                client,
                resource.getExternalId(),
                AliyunBucketSpec::new
        );

        if(bucketSpec.getAclType() != null){
            String aclTypeName = switch (bucketSpec.getAclType()){
                case Private -> "私有";
                case PublicRead -> "公有读";
                case PublicReadWrite -> "公有读写";
                case Default -> "默认";
                default -> "未知";
            };

            RuntimeProperty aclTypeProperty = RuntimeProperty.ofDisplayInList(
                    "aclType",
                    "访问控制类型",
                    bucketSpec.getAclType().name(),
                    aclTypeName
            );
            resource.addOrUpdateRuntimeProperty(aclTypeProperty);
        }

        StorageClass storageClass = bucketSpec.getStorageClass();
        RuntimeProperty storageClassProperty = RuntimeProperty.ofDisplayInList(
                "storageClass",
                "存储类型",
                String.valueOf(storageClass),
                String.valueOf(storageClass)
        );
        resource.addOrUpdateRuntimeProperty(storageClassProperty);

        DataRedundancyType redundancyType = bucketSpec.getRedundancyType();
        RuntimeProperty redundancyProperty = RuntimeProperty.ofDisplayInList(
                "redundancyType",
                "容灾类型",
                String.valueOf(redundancyType),
                String.valueOf(redundancyType)
        );
        resource.addOrUpdateRuntimeProperty(redundancyProperty);
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }

    @Override
    public boolean supportCascadedDestruction() {
        return true;
    }
}
