package com.stratocloud.provider.huawei.obs.actions;

import com.obs.services.model.ObsBucket;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.common.HuaweiCloudClient;
import com.stratocloud.provider.huawei.common.services.HuaweiObsService;
import com.stratocloud.provider.huawei.obs.HuaweiBucketHandler;
import com.stratocloud.provider.huawei.obs.HuaweiBucketSpec;
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
public class HuaweiBucketUpdateHandler implements ResourceActionHandler {

    private final HuaweiBucketHandler bucketHandler;

    public HuaweiBucketUpdateHandler(HuaweiBucketHandler bucketHandler) {
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
        HuaweiCloudProvider provider = (HuaweiCloudProvider) bucketHandler.getProvider();
        HuaweiCloudClient client = provider.buildClient(account);

        HuaweiBucketUpdateInput input = HuaweiBucketSpec.getSpec(
                client,
                resource.getExternalId(),
                HuaweiBucketUpdateInput::new
        );

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(HuaweiBucketUpdateInput.class);
        formMetaData = DynamicFormHelper.changeDefaultValues(formMetaData, input);

        return Optional.of(formMetaData);
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return HuaweiBucketUpdateInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        HuaweiBucketUpdateInput input = JSON.convert(parameters, HuaweiBucketUpdateInput.class);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) bucketHandler.getProvider();
        HuaweiCloudClient client = provider.buildClient(account);
        HuaweiObsService obsService = client.obs();

        ObsBucket obsBucket = obsService.describeBucket(resource.getExternalId()).orElseThrow(
                () -> new StratoException("Bucket not found")
        );

        input.applyStorageClassQuietly(client, obsBucket.getBucketName());
        input.applyVersioningQuietly(client, obsBucket.getBucketName());
        input.applyEncryptionQuietly(client, obsBucket.getBucketName());
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
