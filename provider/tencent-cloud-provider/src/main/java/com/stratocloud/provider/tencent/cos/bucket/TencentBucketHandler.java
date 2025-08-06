package com.stratocloud.provider.tencent.cos.bucket;

import com.qcloud.cos.model.Bucket;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.cos.session.CosSession;
import com.stratocloud.provider.tencent.cos.session.CosSessionKey;
import com.stratocloud.provider.tencent.cos.session.CosSessionManager;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentBucketHandler extends AbstractResourceHandler {

    private final TencentCloudProvider provider;

    public TencentBucketHandler(TencentCloudProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "TENCENT_COS_BUCKET";
    }

    @Override
    public String getResourceTypeName() {
        return "腾讯云存储桶";
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
        TencentCloudClient client = provider.buildClient(account);
        CosSession cosSession = CosSessionManager.getSession(client.getCosSessionKey());
        return cosSession.describeBucket(externalId);
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
        TencentCloudClient client = provider.buildClient(account);
        CosSession cosSession = CosSessionManager.getSession(client.getCosSessionKey());
        return cosSession.describeBuckets().stream().map(
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

        CosSessionKey sessionKey = provider.buildClient(account).getCosSessionKey();
        CosSession cosSession = CosSessionManager.getSession(sessionKey);

        TencentBucketSpec bucketSpec = TencentBucketSpec.retrieveFrom(cosSession, resource);

        if(bucketSpec.getAclType() != null){
            String aclTypeName = switch (bucketSpec.getAclType()){
                case Private -> "私有读写";
                case PublicRead -> "公有读私有写";
                case PublicReadWrite -> "公有读写";
                case Default -> "默认";
            };

            RuntimeProperty aclTypeProperty = RuntimeProperty.ofDisplayInList(
                    "aclType",
                    "访问控制类型",
                    bucketSpec.getAclType().name(),
                    aclTypeName
            );
            resource.addOrUpdateRuntimeProperty(aclTypeProperty);
        }

        String enableMultiAz = bucketSpec.isEnableMultiAz() ? "启用":"未启用";
        RuntimeProperty multiAzProperty = RuntimeProperty.ofDisplayInList(
                "enableMultiAz",
                "多AZ特性",
                String.valueOf(bucketSpec.isEnableMultiAz()),
                enableMultiAz
        );
        resource.addOrUpdateRuntimeProperty(multiAzProperty);
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
