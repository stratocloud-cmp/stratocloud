package com.stratocloud.provider.tencent.cos.lifecycle.actions;

import com.qcloud.cos.model.BucketLifecycleConfiguration;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.cos.bucket.TencentBucketSpec;
import com.stratocloud.provider.tencent.cos.lifecycle.TencentBucketLifecycleRule;
import com.stratocloud.provider.tencent.cos.lifecycle.TencentBucketLifecycleRuleHandler;
import com.stratocloud.provider.tencent.cos.lifecycle.TencentBucketLifecycleRuleId;
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
import java.util.Objects;

@Component
public class TencentBucketLifecycleRuleBuildHandler implements BuildResourceActionHandler {

    private final TencentBucketLifecycleRuleHandler ruleHandler;

    public TencentBucketLifecycleRuleBuildHandler(TencentBucketLifecycleRuleHandler ruleHandler) {
        this.ruleHandler = ruleHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return ruleHandler;
    }

    @Override
    public String getTaskName() {
        return "创建生命周期规则";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return TencentBucketLifecycleRuleBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        validateAndGetBuildRuleTask(resource, parameters).run();
    }


    private Runnable validateAndGetBuildRuleTask(Resource resource, Map<String, Object> parameters){
        TencentCloudProvider provider = (TencentCloudProvider) ruleHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudClient client = provider.buildClient(account);
        CosSessionKey sessionKey = client.getCosSessionKey();
        CosSession cosSession = CosSessionManager.getSession(sessionKey);

        Resource bucketResource = resource.getEssentialTarget(ResourceCategories.BUCKET).orElseThrow(
                () -> new StratoException("Bucket resource not provided")
        );
        String bucketName = bucketResource.getExternalId();

        var input = JSON.convert(parameters, TencentBucketLifecycleRuleBuildInput.class);

        TencentBucketSpec bucketSpec = TencentBucketSpec.retrieveFrom(cosSession, bucketResource);

        List<TencentBucketLifecycleRule> currentRules
                = cosSession.describeBucketLifecycleRulesByBucket(bucketName);

        boolean ruleIdExists = currentRules.stream().anyMatch(
                r -> Objects.equals(r.id().ruleId(), input.getRuleId())
        );
        if(ruleIdExists)
            throw new BadCommandException("规则名称不得重复");

        List<BucketLifecycleConfiguration.Rule> newRules = new ArrayList<>(
                currentRules.stream().map(TencentBucketLifecycleRule::detail).toList()
        );

        var ruleToAdd = new BucketLifecycleConfiguration.Rule();
        input.validateAndApply(ruleToAdd, bucketSpec.isEnableMultiAz());

        newRules.add(ruleToAdd);

        return () -> {
            cosSession.setBucketLifecycle(
                    bucketName,
                    new BucketLifecycleConfiguration(newRules)
            );

            resource.setExternalId(
                    new TencentBucketLifecycleRuleId(
                            bucketName,
                            input.getRuleId()
                    ).toString()
            );
        };
    }


    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        validateAndGetBuildRuleTask(resource, parameters);
    }
}
