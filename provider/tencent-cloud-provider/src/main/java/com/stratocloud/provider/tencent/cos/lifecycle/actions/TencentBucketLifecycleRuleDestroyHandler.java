package com.stratocloud.provider.tencent.cos.lifecycle.actions;

import com.qcloud.cos.model.BucketLifecycleConfiguration;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.resource.DestroyResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.cos.lifecycle.TencentBucketLifecycleRule;
import com.stratocloud.provider.tencent.cos.lifecycle.TencentBucketLifecycleRuleHandler;
import com.stratocloud.provider.tencent.cos.lifecycle.TencentBucketLifecycleRuleId;
import com.stratocloud.provider.tencent.cos.lifecycle.requirements.TencentLifecycleRuleToBucketHandler;
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
public class TencentBucketLifecycleRuleDestroyHandler implements DestroyResourceActionHandler {

    private final TencentBucketLifecycleRuleHandler lifecycleRuleHandler;

    public TencentBucketLifecycleRuleDestroyHandler(TencentBucketLifecycleRuleHandler lifecycleRuleHandler) {
        this.lifecycleRuleHandler = lifecycleRuleHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return lifecycleRuleHandler;
    }

    @Override
    public String getTaskName() {
        return "删除存储桶生命周期";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        TencentCloudProvider provider = (TencentCloudProvider) lifecycleRuleHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<TencentBucketLifecycleRule> rule = lifecycleRuleHandler.describeBucketLifecycleRule(
                account, resource.getExternalId()
        );

        if(rule.isEmpty())
            return;

        TencentCloudClient client = provider.buildClient(account);
        CosSessionKey sessionKey = client.getCosSessionKey();
        CosSession cosSession = CosSessionManager.getSession(sessionKey);

        TencentBucketLifecycleRuleId ruleId = rule.get().id();

        var keepingRules = cosSession.describeBucketLifecycleRulesByBucket(
                ruleId.bucketName()
        ).stream().map(
                TencentBucketLifecycleRule::detail
        ).filter(
                r -> !Objects.equals(r.getId(), ruleId.ruleId())
        ).toList();

        if(Utils.isEmpty(keepingRules))
            cosSession.deleteBucketLifecycle(ruleId.bucketName());
        else
            cosSession.setBucketLifecycle(
                    ruleId.bucketName(),
                    new BucketLifecycleConfiguration(keepingRules)
            );
    }

    @Override
    public List<String> getLockExclusiveTargetRelTypeIds() {
        return List.of(
                TencentLifecycleRuleToBucketHandler.TYPE_ID
        );
    }
}
