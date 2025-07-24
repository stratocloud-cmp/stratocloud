package com.stratocloud.provider.tencent.cos.lifecycle;

import com.qcloud.cos.model.BucketLifecycleConfiguration;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.provider.AbstractResourceHandler;
import com.stratocloud.provider.Provider;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.cos.session.CosSession;
import com.stratocloud.provider.tencent.cos.session.CosSessionManager;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TencentBucketLifecycleRuleHandler extends AbstractResourceHandler {

    private final TencentCloudProvider provider;

    public TencentBucketLifecycleRuleHandler(TencentCloudProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "TENCENT_COS_BUCKET_LIFECYCLE_RULE";
    }

    @Override
    public String getResourceTypeName() {
        return "腾讯云存储桶生命周期";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.BUCKET_LIFECYCLE;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return describeBucketLifecycleRule(account, externalId).map(
                rule -> toExternalResource(account, rule)
        );
    }

    public Optional<TencentBucketLifecycleRule> describeBucketLifecycleRule(ExternalAccount account,
                                                                            String externalId){
        if(Utils.isBlank(externalId))
            return Optional.empty();
        TencentCloudClient client = provider.buildClient(account);
        CosSession cosSession = CosSessionManager.getSession(client.getCosSessionKey());
        return cosSession.describeBucketLifecycleRule(TencentBucketLifecycleRuleId.fromString(externalId));
    }

    private ExternalResource toExternalResource(ExternalAccount account,
                                                TencentBucketLifecycleRule rule){
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                rule.id().toString(),
                rule.id().ruleId(),
                convertStatus(rule)
        );
    }

    private ResourceState convertStatus(TencentBucketLifecycleRule rule) {
        String status = rule.detail().getStatus();
        if(Utils.isBlank(status))
            return ResourceState.UNKNOWN;
        return switch (status){
            case BucketLifecycleConfiguration.ENABLED -> ResourceState.ENABLED;
            case BucketLifecycleConfiguration.DISABLED -> ResourceState.DISABLED;
            default -> ResourceState.UNKNOWN;
        };
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        TencentCloudClient client = provider.buildClient(account);
        CosSession cosSession = CosSessionManager.getSession(client.getCosSessionKey());
        return cosSession.describeBucketLifecycleRules().stream().map(
                r -> toExternalResource(account, r)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        var rule = describeBucketLifecycleRule(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Bucket not found")
        );
        resource.updateByExternal(toExternalResource(account, rule));

        String filterDescription = rule.getFilterDescription();
        RuntimeProperty filterProperty = RuntimeProperty.ofDisplayInList(
                "filter",
                "应用范围",
                filterDescription,
                filterDescription
        );
        resource.addOrUpdateRuntimeProperty(filterProperty);

        String contentDescription = rule.getContentDescription();
        RuntimeProperty contentProperty = RuntimeProperty.ofDisplayInList(
                "content",
                "规则内容",
                contentDescription,
                contentDescription
        );
        resource.addOrUpdateRuntimeProperty(contentProperty);
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
