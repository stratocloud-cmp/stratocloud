package com.stratocloud.provider.tencent.cos.bucket.actions;

import com.qcloud.cos.model.Bucket;
import com.qcloud.cos.model.BucketLoggingConfiguration;
import com.stratocloud.account.ExternalAccount;
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
public class TencentBucketUpdateLoggingHandler implements ResourceActionHandler {

    private final TencentBucketHandler bucketHandler;

    public TencentBucketUpdateLoggingHandler(TencentBucketHandler bucketHandler) {
        this.bucketHandler = bucketHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return bucketHandler;
    }

    @Override
    public ResourceAction getAction() {
        return BucketActions.UPDATE_LOGGING;
    }

    @Override
    public String getTaskName() {
        return "配置存储桶日志转存";
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

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(TencentBucketUpdateLoggingInput.class);
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) bucketHandler.getProvider();
        CosSession cosSession = CosSessionManager.getSession(provider.buildClient(account).getCosSessionKey());

        var logging = cosSession.describeBucketLogging(resource.getExternalId());

        TencentBucketUpdateLoggingInput input = new TencentBucketUpdateLoggingInput();
        if(logging.isEmpty() || Utils.isBlank(logging.get().getDestinationBucketName())) {
            input.setEnableLogging(false);
        } else {
            input.setEnableLogging(true);
            input.setLoggingTargetBucketName(logging.get().getDestinationBucketName());
            input.setLoggingFilePrefix(logging.get().getLogFilePrefix());
        }

        formMetaData = DynamicFormHelper.changeDefaultValues(
                formMetaData,
                input
        );

        List<String> bucketNames = cosSession.describeBuckets().stream().map(Bucket::getName).toList();

        formMetaData = DynamicFormHelper.changeOptions(
                formMetaData, "loggingTargetBucketName", bucketNames, bucketNames
        );

        return Optional.of(formMetaData);
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return TencentBucketUpdateLoggingInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        TencentBucketUpdateLoggingInput input = JSON.convert(parameters, TencentBucketUpdateLoggingInput.class);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) bucketHandler.getProvider();
        CosSessionKey sessionKey = provider.buildClient(account).getCosSessionKey();
        CosSession cosSession = CosSessionManager.getSession(sessionKey);

        if(input.isEnableLogging())
            cosSession.setBucketLogging(resource.getExternalId(), input.toConfig());
        else
            cosSession.setBucketLogging(resource.getExternalId(), new BucketLoggingConfiguration());
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
