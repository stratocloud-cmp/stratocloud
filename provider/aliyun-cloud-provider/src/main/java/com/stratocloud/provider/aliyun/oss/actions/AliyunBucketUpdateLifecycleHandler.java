package com.stratocloud.provider.aliyun.oss.actions;

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
public class AliyunBucketUpdateLifecycleHandler implements ResourceActionHandler {

    private final AliyunBucketHandler bucketHandler;

    public AliyunBucketUpdateLifecycleHandler(AliyunBucketHandler bucketHandler) {
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
    public Class<? extends ResourceActionInput> getInputClass() {
        return AliyunBucketUpdateLifecycleInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        if(Utils.isBlank(resource.getExternalId()))
            return Optional.empty();

        AliyunCloudProvider provider = (AliyunCloudProvider) bucketHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunBucketUpdateLifecycleInput input = AliyunBucketUpdateLifecycleInput.getInput(
                provider.buildClient(account), resource.getExternalId()
        );

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(AliyunBucketUpdateLifecycleInput.class);
        formMetaData = DynamicFormHelper.changeDefaultValues(formMetaData, input);
        return Optional.of(formMetaData);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        AliyunBucketUpdateLifecycleInput input = JSON.convert(parameters, AliyunBucketUpdateLifecycleInput.class);

        AliyunCloudProvider provider = (AliyunCloudProvider) bucketHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunOssService ossService = provider.buildClient(account).oss();

        ossService.setAccessMonitor(resource.getExternalId(), input.isEnableAccessMonitor());

        if(input.isEnabled() && Utils.isNotEmpty(input.getRules())){
            ossService.setBucketLifecycle(resource.getExternalId(), input.toLifecycleRules());
        } else {
            ossService.deleteBucketLifecycle(resource.getExternalId());
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
        AliyunBucketUpdateLifecycleInput input = JSON.convert(parameters, AliyunBucketUpdateLifecycleInput.class);
        input.validate();
    }
}
