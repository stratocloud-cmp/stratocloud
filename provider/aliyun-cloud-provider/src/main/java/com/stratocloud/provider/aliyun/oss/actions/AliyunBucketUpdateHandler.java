package com.stratocloud.provider.aliyun.oss.actions;

import com.aliyun.oss.model.Bucket;
import com.aliyun.oss.model.CreateBucketRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.aliyun.oss.AliyunBucketHandler;
import com.stratocloud.provider.aliyun.oss.AliyunBucketSpec;
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
public class AliyunBucketUpdateHandler implements ResourceActionHandler {

    private final AliyunBucketHandler bucketHandler;

    public AliyunBucketUpdateHandler(AliyunBucketHandler bucketHandler) {
        this.bucketHandler = bucketHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return bucketHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.UPDATE;
    }

    @Override
    public String getTaskName() {
        return "更新存储桶";
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

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) bucketHandler.getProvider();
        AliyunClient client = provider.buildClient(account);

        AliyunBucketUpdateInput input = AliyunBucketSpec.getSpec(
                client,
                resource.getExternalId(),
                AliyunBucketUpdateInput::new
        );

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(AliyunBucketUpdateInput.class);

        formMetaData = DynamicFormHelper.changeDefaultValues(formMetaData, input);
        return Optional.of(formMetaData);
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return AliyunBucketUpdateInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        AliyunBucketUpdateInput input = JSON.convert(parameters, AliyunBucketUpdateInput.class);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) bucketHandler.getProvider();
        AliyunClient client = provider.buildClient(account);

        CreateBucketRequest request = new CreateBucketRequest(resource.getName());
        request.setCannedACL(request.getCannedACL());
        request.setStorageClass(request.getStorageClass());
        request.setDataRedundancyType(request.getDataRedundancyType());

        Bucket bucket = client.oss().createBucket(request);
        resource.setExternalId(bucket.getName());

        input.applyVersioningQuietly(client, bucket.getName());
        input.applyEncryptionQuietly(client, bucket.getName());
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
