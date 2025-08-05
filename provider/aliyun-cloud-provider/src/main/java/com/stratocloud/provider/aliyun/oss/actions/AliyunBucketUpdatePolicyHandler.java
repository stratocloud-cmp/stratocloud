package com.stratocloud.provider.aliyun.oss.actions;

import com.aliyun.oss.model.GetBucketPolicyResult;
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
public class AliyunBucketUpdatePolicyHandler implements ResourceActionHandler {

    private final AliyunBucketHandler bucketHandler;

    public AliyunBucketUpdatePolicyHandler(AliyunBucketHandler bucketHandler) {
        this.bucketHandler = bucketHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return bucketHandler;
    }

    @Override
    public ResourceAction getAction() {
        return BucketActions.UPDATE_POLICY;
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

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(AliyunBucketUpdatePolicyInput.class);
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) bucketHandler.getProvider();
        AliyunOssService ossService = provider.buildClient(account).oss();

        Optional<GetBucketPolicyResult> policy = ossService.describeBucketPolicy(resource.getExternalId());

        AliyunBucketUpdatePolicyInput input = new AliyunBucketUpdatePolicyInput();
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
        return AliyunBucketUpdatePolicyInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        AliyunBucketUpdatePolicyInput input = JSON.convert(parameters, AliyunBucketUpdatePolicyInput.class);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) bucketHandler.getProvider();
        AliyunOssService ossService = provider.buildClient(account).oss();

        if(input.isEnabled())
            ossService.setBucketPolicy(resource.getExternalId(), input.getPolicyText());
        else
            ossService.deleteBucketPolicy(resource.getExternalId());
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
