package com.stratocloud.provider.tencent.cos.cors.actions;

import com.qcloud.cos.model.BucketCrossOriginConfiguration;
import com.qcloud.cos.model.CORSRule;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.cos.cors.TencentBucketCorsRule;
import com.stratocloud.provider.tencent.cos.cors.TencentBucketCorsRuleHandler;
import com.stratocloud.provider.tencent.cos.cors.TencentBucketCorsRuleId;
import com.stratocloud.provider.tencent.cos.cors.requirements.TencentCorsRuleToBucketHandler;
import com.stratocloud.provider.tencent.cos.session.CosSession;
import com.stratocloud.provider.tencent.cos.session.CosSessionKey;
import com.stratocloud.provider.tencent.cos.session.CosSessionManager;
import com.stratocloud.resource.Resource;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class TencentBucketCorsRuleDestroyHandler implements DestroyResourceActionHandler {

    private final TencentBucketCorsRuleHandler corsRuleHandler;

    public TencentBucketCorsRuleDestroyHandler(TencentBucketCorsRuleHandler corsRuleHandler) {
        this.corsRuleHandler = corsRuleHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return corsRuleHandler;
    }

    @Override
    public String getTaskName() {
        return "删除存储桶CORS";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        TencentCloudProvider provider = (TencentCloudProvider) corsRuleHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<TencentBucketCorsRule> rule = corsRuleHandler.describeBucketCorsRule(
                account, resource.getExternalId()
        );

        if(rule.isEmpty())
            return;

        TencentCloudClient client = provider.buildClient(account);
        CosSessionKey sessionKey = client.getCosSessionKey();
        CosSession cosSession = CosSessionManager.getSession(sessionKey);

        TencentBucketCorsRuleId ruleId = rule.get().id();

        List<CORSRule> keepingRules = cosSession.describeBucketCorsRulesByBucket(
                ruleId.bucketName()
        ).stream().map(
                TencentBucketCorsRule::detail
        ).filter(
                r -> !Objects.equals(r.getId(), ruleId.ruleId())
        ).toList();

        if(Utils.isEmpty(keepingRules))
            cosSession.deleteBucketCors(ruleId.bucketName());
        else
            cosSession.setBucketCors(
                    ruleId.bucketName(),
                    new BucketCrossOriginConfiguration(keepingRules)
            );
    }

    @Override
    public List<String> getLockExclusiveTargetRelTypeIds() {
        return List.of(
                TencentCorsRuleToBucketHandler.TYPE_ID
        );
    }
}
