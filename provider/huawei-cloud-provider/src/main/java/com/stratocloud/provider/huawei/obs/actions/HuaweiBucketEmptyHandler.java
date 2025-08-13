package com.stratocloud.provider.huawei.obs.actions;

import com.obs.services.model.KeyAndVersion;
import com.obs.services.model.VersionOrDeleteMarker;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.constants.BucketActions;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.common.services.HuaweiObsService;
import com.stratocloud.provider.huawei.obs.HuaweiBucketHandler;
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
public class HuaweiBucketEmptyHandler implements ResourceActionHandler {

    private final HuaweiBucketHandler bucketHandler;

    public HuaweiBucketEmptyHandler(HuaweiBucketHandler bucketHandler) {
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
        HuaweiCloudProvider provider = (HuaweiCloudProvider) bucketHandler.getProvider();
        HuaweiObsService obsService = provider.buildClient(account).obs();

        if(!obsService.doesBucketExist(bucketName))
            return;

        List<VersionOrDeleteMarker> versionSummaries = obsService.describeObjectVersions(bucketName);

        if(Utils.isEmpty(versionSummaries))
            return;

        for (List<VersionOrDeleteMarker> partition : Utils.partition(versionSummaries, 1000)) {
            obsService.deleteVersions(
                    bucketName,
                    partition.stream().map(
                            v -> new KeyAndVersion(v.getKey(), v.getVersionId())
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
