package com.stratocloud.provider.tencent.cos.lifecycle.actions;

import com.qcloud.cos.model.BucketLifecycleConfiguration;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.resource.ResourceActionHandler;
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
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TencentBucketLifecycleRuleUpdateHandler implements ResourceActionHandler {

    private final TencentBucketLifecycleRuleHandler ruleHandler;

    public TencentBucketLifecycleRuleUpdateHandler(TencentBucketLifecycleRuleHandler ruleHandler) {
        this.ruleHandler = ruleHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return ruleHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.UPDATE;
    }

    @Override
    public String getTaskName() {
        return "更新生命周期规则";
    }

    @Override
    public Set<ResourceState> getAllowedStates() {
        return ResourceState.getAliveStateSet();
    }

    @Override
    public Optional<ResourceState> getTransitionState() {
        return Optional.of(ResourceState.CONFIGURING);
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        DynamicFormMetaData formMetaData
                = DynamicFormHelper.generateMetaData(TencentBucketLifecycleRuleUpdateInput.class);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        var rule = ruleHandler.describeBucketLifecycleRule(account, resource.getExternalId());

        if(rule.isEmpty())
            return Optional.empty();

        TencentBucketLifecycleRuleUpdateInput input = TencentBucketLifecycleRuleSpec.getSpec(
                rule.get().detail(),
                TencentBucketLifecycleRuleUpdateInput::new
        );

        formMetaData = DynamicFormHelper.changeDefaultValues(formMetaData, input);

        return Optional.of(formMetaData);
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return TencentBucketLifecycleRuleUpdateInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        validateAndGetUpdateRuleTask(resource, parameters).run();
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        return ResourceActionResult.finished();
    }


    private Runnable validateAndGetUpdateRuleTask(Resource resource, Map<String, Object> parameters){
        TencentCloudProvider provider = (TencentCloudProvider) ruleHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudClient client = provider.buildClient(account);
        CosSessionKey sessionKey = client.getCosSessionKey();
        CosSession cosSession = CosSessionManager.getSession(sessionKey);

        Resource bucketResource = resource.getEssentialTarget(ResourceCategories.BUCKET).orElseThrow(
                () -> new StratoException("Bucket resource not provided")
        );
        String bucketName = bucketResource.getExternalId();

        var input = JSON.convert(parameters, TencentBucketLifecycleRuleUpdateInput.class);

        TencentBucketSpec bucketSpec = TencentBucketSpec.retrieveFrom(cosSession, bucketResource);

        TencentBucketLifecycleRuleId ruleId = TencentBucketLifecycleRuleId.fromString(resource.getExternalId());

        List<TencentBucketLifecycleRule> currentRules
                = cosSession.describeBucketLifecycleRulesByBucket(bucketName);

        List<BucketLifecycleConfiguration.Rule> rules = new ArrayList<>(
                currentRules.stream().map(TencentBucketLifecycleRule::detail).toList()
        );

        var ruleToUpdate = rules.stream().filter(
                r -> Objects.equals(r.getId(), ruleId.ruleId())
        ).findAny().orElseThrow(
                () -> new StratoException("Lifecycle rule not found")
        );
        input.validateAndApply(ruleToUpdate, bucketSpec.isEnableMultiAz());

        return () -> cosSession.setBucketLifecycle(
                bucketName,
                new BucketLifecycleConfiguration(rules)
        );
    }


    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        validateAndGetUpdateRuleTask(resource, parameters);
    }
}
