package com.stratocloud.provider.tencent.cos.cors;

import com.qcloud.cos.model.CORSRule;
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
public class TencentBucketCorsRuleHandler extends AbstractResourceHandler {

    private final TencentCloudProvider provider;

    public TencentBucketCorsRuleHandler(TencentCloudProvider provider) {
        this.provider = provider;
    }

    @Override
    public Provider getProvider() {
        return provider;
    }

    @Override
    public String getResourceTypeId() {
        return "TENCENT_COS_BUCKET_CORS_RULE";
    }

    @Override
    public String getResourceTypeName() {
        return "腾讯云存储桶CORS";
    }

    @Override
    public ResourceCategory getResourceCategory() {
        return ResourceCategories.BUCKET_CORS;
    }

    @Override
    public boolean isInfrastructure() {
        return false;
    }

    @Override
    public Optional<ExternalResource> describeExternalResource(ExternalAccount account, String externalId) {
        return describeBucketCorsRule(account, externalId).map(
                rule -> toExternalResource(account, rule)
        );
    }

    public Optional<TencentBucketCorsRule> describeBucketCorsRule(ExternalAccount account, String externalId){
        if(Utils.isBlank(externalId))
            return Optional.empty();
        TencentCloudClient client = provider.buildClient(account);
        CosSession cosSession = CosSessionManager.getSession(client.getCosSessionKey());
        return cosSession.describeBucketCorsRule(TencentBucketCorsRuleId.fromString(externalId));
    }

    private ExternalResource toExternalResource(ExternalAccount account,
                                                TencentBucketCorsRule rule){
        return new ExternalResource(
                provider.getId(),
                account.getId(),
                getResourceCategory().id(),
                getResourceTypeId(),
                rule.id().toString(),
                rule.id().ruleId(),
                ResourceState.IN_USE
        );
    }

    @Override
    public List<ExternalResource> describeExternalResources(ExternalAccount account, Map<String, Object> queryArgs) {
        TencentCloudClient client = provider.buildClient(account);
        CosSession cosSession = CosSessionManager.getSession(client.getCosSessionKey());
        return cosSession.describeBucketCorsRules().stream().map(
                r -> toExternalResource(account, r)
        ).toList();
    }

    @Override
    public void synchronize(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        var rule = describeBucketCorsRule(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Bucket not found")
        );
        resource.updateByExternal(toExternalResource(account, rule));

        CORSRule corsRule = rule.detail();

        List<String> allowedOrigins = corsRule.getAllowedOrigins();
        if(Utils.isNotEmpty(allowedOrigins)){
            String allowedOriginsStr = String.join(",", allowedOrigins);
            resource.addOrUpdateRuntimeProperty(
                    RuntimeProperty.ofDisplayInList(
                            "allowedOrigins", "AllowedOrigin", allowedOriginsStr, allowedOriginsStr
                    )
            );
        }

        List<CORSRule.AllowedMethods> allowedMethods = corsRule.getAllowedMethods();
        if(Utils.isNotEmpty(allowedMethods)){
            String allowedMethodsStr = String.join(
                    ",",
                    allowedMethods.stream().map(CORSRule.AllowedMethods::toString).toList()
            );
            resource.addOrUpdateRuntimeProperty(
                    RuntimeProperty.ofDisplayInList(
                            "allowedMethods", "AllowedMethod", allowedMethodsStr, allowedMethodsStr
                    )
            );
        }

        List<String> allowedHeaders = corsRule.getAllowedHeaders();
        if(Utils.isNotEmpty(allowedHeaders)){
            String allowedHeadersStr = String.join(",", allowedHeaders);
            resource.addOrUpdateRuntimeProperty(
                    RuntimeProperty.ofDisplayInList(
                            "allowedHeaders", "AllowedHeader", allowedHeadersStr, allowedHeadersStr
                    )
            );
        }

        List<String> exposedHeaders = corsRule.getExposedHeaders();
        if(Utils.isNotEmpty(exposedHeaders)){
            String exposedHeadersStr = String.join(",", exposedHeaders);
            resource.addOrUpdateRuntimeProperty(
                    RuntimeProperty.ofDisplayInList(
                            "exposedHeaders", "ExposedHeader", exposedHeadersStr, exposedHeadersStr
                    )
            );
        }

        String maxAgeStr = String.valueOf(corsRule.getMaxAgeSeconds());
        resource.addOrUpdateRuntimeProperty(
                RuntimeProperty.ofDisplayInList(
                        "maxAgeSeconds", "超时Max-Age(秒)", maxAgeStr, maxAgeStr
                )
        );
    }

    @Override
    public List<ResourceUsageType> getUsagesTypes() {
        return List.of();
    }
}
