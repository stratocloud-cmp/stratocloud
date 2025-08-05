package com.stratocloud.provider.aliyun.oss.actions;

import com.aliyun.oss.model.Bucket;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.services.AliyunOssService;
import com.stratocloud.provider.aliyun.oss.AliyunBucketHandler;
import com.stratocloud.provider.constants.BucketActions;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class AliyunBucketUpdateLoggingHandler implements ResourceActionHandler {

    private final AliyunBucketHandler bucketHandler;

    public AliyunBucketUpdateLoggingHandler(AliyunBucketHandler bucketHandler) {
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
    public Class<? extends ResourceActionInput> getInputClass() {
        return AliyunBucketUpdateLoggingInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        if(Utils.isBlank(resource.getExternalId()))
            return Optional.empty();

        AliyunCloudProvider provider = (AliyunCloudProvider) bucketHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunOssService ossService = provider.buildClient(account).oss();

        var logging = ossService.describeBucketLogging(resource.getExternalId());

        AliyunBucketUpdateLoggingInput input = new AliyunBucketUpdateLoggingInput();

        if(logging.isPresent()){
            input.setEnabled(true);
            input.setTargetBucket(logging.get().getTargetBucket());
            input.setTargetPrefix(logging.get().getTargetPrefix());
        }else {
            input.setEnabled(false);
        }

        List<String> bucketNames = ossService.describeBuckets().stream().map(Bucket::getName).toList();

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(AliyunBucketUpdateLoggingInput.class);

        formMetaData = DynamicFormHelper.changeDefaultValues(formMetaData, input);

        formMetaData = DynamicFormHelper.changeOptions(formMetaData, "targetBucket", bucketNames, bucketNames);

        return Optional.of(formMetaData);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        AliyunBucketUpdateLoggingInput input = JSON.convert(parameters, AliyunBucketUpdateLoggingInput.class);

        AliyunCloudProvider provider = (AliyunCloudProvider) bucketHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunOssService ossService = provider.buildClient(account).oss();

        if(input.isEnabled()){
            ossService.setBucketLogging(resource.getExternalId(), input.getTargetBucket(), input.getTargetPrefix());
        } else {
            ossService.deleteBucketLogging(resource.getExternalId());
        }
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
