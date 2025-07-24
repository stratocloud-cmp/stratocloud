package com.stratocloud.provider.tencent.cos.cors.actions;

import com.qcloud.cos.model.BucketCrossOriginConfiguration;
import com.qcloud.cos.model.CORSRule;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.cos.cors.TencentBucketCorsRule;
import com.stratocloud.provider.tencent.cos.cors.TencentBucketCorsRuleHandler;
import com.stratocloud.provider.tencent.cos.cors.requirements.TencentCorsRuleToBucketHandler;
import com.stratocloud.provider.tencent.cos.session.CosSession;
import com.stratocloud.provider.tencent.cos.session.CosSessionKey;
import com.stratocloud.provider.tencent.cos.session.CosSessionManager;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class TencentBucketCorsRuleBuildHandler implements BuildResourceActionHandler {

    private final TencentBucketCorsRuleHandler corsRuleHandler;

    public TencentBucketCorsRuleBuildHandler(TencentBucketCorsRuleHandler corsRuleHandler) {
        this.corsRuleHandler = corsRuleHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return corsRuleHandler;
    }

    @Override
    public String getTaskName() {
        return "创建存储桶CORS";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return TencentBucketCorsRuleBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        TencentBucketCorsRuleBuildInput input = JSON.convert(parameters, TencentBucketCorsRuleBuildInput.class);

        Resource bucketResource = resource.getEssentialTarget(ResourceCategories.BUCKET).orElseThrow(
                () -> new StratoException("Bucket resource not provided")
        );

        TencentCloudProvider provider = (TencentCloudProvider) corsRuleHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudClient client = provider.buildClient(account);
        CosSessionKey sessionKey = client.getCosSessionKey();
        CosSession cosSession = CosSessionManager.getSession(sessionKey);

        List<CORSRule> rules = cosSession.describeBucketCorsRulesByBucket(
                bucketResource.getExternalId()
        ).stream().map(TencentBucketCorsRule::detail).toList();

        List<CORSRule> newRules = new ArrayList<>(rules);

        CORSRule newRule = new CORSRule();
        newRule.setId(input.getRuleId());
        newRule.setAllowedMethods(input.getAllowedMethods());
        newRule.setAllowedOrigins(input.getAllowedOrigins());
        newRule.setMaxAgeSeconds(input.getMaxAgeSeconds());
        newRule.setExposedHeaders(input.getExposedHeaders());
        newRule.setAllowedHeaders(input.getAllowedHeaders());

        newRules.add(newRule);

        cosSession.setBucketCors(
                bucketResource.getExternalId(),
                new BucketCrossOriginConfiguration(newRules)
        );
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }

    @Override
    public List<String> getLockExclusiveTargetRelTypeIds() {
        return List.of(
                TencentCorsRuleToBucketHandler.TYPE_ID
        );
    }
}
