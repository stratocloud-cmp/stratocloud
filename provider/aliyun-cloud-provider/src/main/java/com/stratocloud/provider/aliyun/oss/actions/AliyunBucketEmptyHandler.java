package com.stratocloud.provider.aliyun.oss.actions;

import com.aliyun.oss.model.DeleteVersionsRequest;
import com.aliyun.oss.model.OSSVersionSummary;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.services.AliyunOssService;
import com.stratocloud.provider.aliyun.oss.AliyunBucketHandler;
import com.stratocloud.provider.constants.BucketActions;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class AliyunBucketEmptyHandler implements ResourceActionHandler {

    private final AliyunBucketHandler bucketHandler;

    public AliyunBucketEmptyHandler(AliyunBucketHandler bucketHandler) {
        this.bucketHandler = bucketHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return bucketHandler;
    }

    @Override
    public ResourceAction getAction() {
        return BucketActions.EMPTY_BUCKET;
    }

    @Override
    public String getTaskName() {
        return "清空存储桶";
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
        return ResourceActionInput.Dummy.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        String bucketName = resource.getExternalId();
        if(Utils.isBlank(bucketName))
            return;

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) bucketHandler.getProvider();
        AliyunOssService ossService = provider.buildClient(account).oss();

        if(!ossService.doesBucketExist(bucketName))
            return;

        List<OSSVersionSummary> versionSummaries = ossService.describeObjectVersions(bucketName);

        if(Utils.isEmpty(versionSummaries))
            return;

        for (List<OSSVersionSummary> partition : Utils.partition(versionSummaries, 1000)) {
            ossService.deleteVersions(
                    bucketName,
                    partition.stream().map(
                            v -> new DeleteVersionsRequest.KeyVersion(v.getKey(), v.getVersionId())
                    ).toList()
            );
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
