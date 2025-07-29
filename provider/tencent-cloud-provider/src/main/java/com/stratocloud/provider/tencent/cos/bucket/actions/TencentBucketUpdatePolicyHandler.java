package com.stratocloud.provider.tencent.cos.bucket.actions;

import com.qcloud.cos.model.BucketPolicy;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.cos.bucket.TencentBucketHandler;
import com.stratocloud.provider.tencent.cos.session.CosSession;
import com.stratocloud.provider.tencent.cos.session.CosSessionKey;
import com.stratocloud.provider.tencent.cos.session.CosSessionManager;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class TencentBucketUpdatePolicyHandler implements ResourceActionHandler {

    private final TencentBucketHandler bucketHandler;

    public TencentBucketUpdatePolicyHandler(TencentBucketHandler bucketHandler) {
        this.bucketHandler = bucketHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return bucketHandler;
    }

    @Override
    public ResourceAction getAction() {
        return new ResourceAction(
                "UPDATE_BUCKET_POLICY",
                "配置访问策略",
                501
        );
    }

    @Override
    public String getTaskName() {
        return "配置存储桶访问策略";
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
        if(Utils.isBlank(resource.getExternalId()))
            return Optional.empty();

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(TencentBucketUpdatePolicyInput.class);
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) bucketHandler.getProvider();
        CosSession cosSession = CosSessionManager.getSession(provider.buildClient(account).getCosSessionKey());

        Optional<BucketPolicy> policy = cosSession.describeBucketPolicy(resource.getExternalId());

        TencentBucketUpdatePolicyInput input = new TencentBucketUpdatePolicyInput();
        if(policy.isEmpty()) {
            input.setEnabled(false);
        } else {
            input.setEnabled(true);
            input.setPolicyText(policy.get().getPolicyText());
        }

        formMetaData = DynamicFormHelper.changeDefaultValues(
                formMetaData,
                input
        );

        return Optional.of(formMetaData);
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return TencentBucketUpdatePolicyInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        TencentBucketUpdatePolicyInput input = JSON.convert(parameters, TencentBucketUpdatePolicyInput.class);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) bucketHandler.getProvider();
        CosSessionKey sessionKey = provider.buildClient(account).getCosSessionKey();
        CosSession cosSession = CosSessionManager.getSession(sessionKey);

        if(input.isEnabled())
            cosSession.setBucketPolicy(resource.getExternalId(), input.getPolicyText());
        else
            cosSession.deleteBucketPolicy(resource.getExternalId());
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        return ResourceActionResult.finished();
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }
}
