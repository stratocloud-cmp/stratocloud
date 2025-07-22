package com.stratocloud.provider.tencent.cos.bucket.actions;

import com.qcloud.cos.model.COSVersionSummary;
import com.qcloud.cos.model.DeleteObjectsRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.cos.bucket.TencentBucketHandler;
import com.stratocloud.provider.tencent.cos.session.CosSession;
import com.stratocloud.provider.tencent.cos.session.CosSessionKey;
import com.stratocloud.provider.tencent.cos.session.CosSessionManager;
import com.stratocloud.resource.*;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class TencentBucketEmptyHandler implements ResourceActionHandler {

    private final TencentBucketHandler bucketHandler;

    public TencentBucketEmptyHandler(TencentBucketHandler bucketHandler) {
        this.bucketHandler = bucketHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return bucketHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.EMPTY;
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
        TencentCloudProvider provider = (TencentCloudProvider) bucketHandler.getProvider();
        CosSessionKey sessionKey = provider.buildClient(account).getCosSessionKey();

        CosSession cosSession = CosSessionManager.getSession(sessionKey);

        if(!cosSession.doesBucketExist(bucketName))
            return;

        List<COSVersionSummary> versionSummaries = cosSession.describeObjectVersions(bucketName);

        if(Utils.isEmpty(versionSummaries))
            return;

        for (List<COSVersionSummary> partition : Utils.partition(versionSummaries, 1000)) {
            cosSession.deleteObjects(
                    bucketName,
                    partition.stream().map(
                            v -> new DeleteObjectsRequest.KeyVersion(v.getKey(), v.getVersionId())
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
