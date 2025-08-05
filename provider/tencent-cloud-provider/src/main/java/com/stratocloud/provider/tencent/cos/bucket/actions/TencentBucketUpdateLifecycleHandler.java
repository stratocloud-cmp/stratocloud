package com.stratocloud.provider.tencent.cos.bucket.actions;

import com.qcloud.cos.model.HeadBucketResult;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.constants.BucketActions;
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
public class TencentBucketUpdateLifecycleHandler implements ResourceActionHandler {

    private final TencentBucketHandler bucketHandler;

    public TencentBucketUpdateLifecycleHandler(TencentBucketHandler bucketHandler) {
        this.bucketHandler = bucketHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return bucketHandler;
    }

    @Override
    public ResourceAction getAction() {
        return BucketActions.UPDATE_LIFECYCLE;
    }

    @Override
    public String getTaskName() {
        return "配置存储桶生命周期";
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

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(TencentBucketUpdateLifecycleInput.class);
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) bucketHandler.getProvider();
        CosSession cosSession = CosSessionManager.getSession(provider.buildClient(account).getCosSessionKey());

        TencentBucketUpdateLifecycleInput input = TencentBucketUpdateLifecycleInput.getInput(
                cosSession, resource.getExternalId()
        );

        formMetaData = DynamicFormHelper.changeDefaultValues(
                formMetaData,
                input
        );

        return Optional.of(formMetaData);
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return TencentBucketUpdateLifecycleInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        TencentBucketUpdateLifecycleInput input = JSON.convert(parameters, TencentBucketUpdateLifecycleInput.class);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) bucketHandler.getProvider();
        CosSessionKey sessionKey = provider.buildClient(account).getCosSessionKey();
        CosSession cosSession = CosSessionManager.getSession(sessionKey);

        HeadBucketResult headBucketResult = cosSession.headBucket(resource.getExternalId()).orElseThrow(
                () -> new StratoException("Bucket not found")
        );

        if(input.isEnabled() && Utils.isNotEmpty(input.getRules()))
            cosSession.setBucketLifecycle(resource.getExternalId(), input.toConfig(headBucketResult.isMazBucket()));
        else
            cosSession.deleteBucketLifecycle(resource.getExternalId());
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
        JSON.convert(parameters, TencentBucketUpdateLifecycleInput.class).validate();
    }
}
