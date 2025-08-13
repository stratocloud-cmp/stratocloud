package com.stratocloud.provider.huawei.obs.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.constants.BucketActions;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.common.services.HuaweiObsService;
import com.stratocloud.provider.huawei.obs.HuaweiBucketHandler;
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
public class HuaweiBucketUpdateLifecycleHandler implements ResourceActionHandler {

    private final HuaweiBucketHandler bucketHandler;

    public HuaweiBucketUpdateLifecycleHandler(HuaweiBucketHandler bucketHandler) {
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
        return HuaweiBucketUpdateLifecycleInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        if(Utils.isBlank(resource.getExternalId()))
            return Optional.empty();

        HuaweiCloudProvider provider = (HuaweiCloudProvider) bucketHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiBucketUpdateLifecycleInput input = HuaweiBucketUpdateLifecycleInput.getInput(
                provider.buildClient(account), resource.getExternalId()
        );

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(HuaweiBucketUpdateLifecycleInput.class);
        formMetaData = DynamicFormHelper.changeDefaultValues(formMetaData, input);
        return Optional.of(formMetaData);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        HuaweiBucketUpdateLifecycleInput input = JSON.convert(parameters, HuaweiBucketUpdateLifecycleInput.class);

        HuaweiCloudProvider provider = (HuaweiCloudProvider) bucketHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiObsService obsService = provider.buildClient(account).obs();

        if(input.isEnabled() && Utils.isNotEmpty(input.getRules())){
            obsService.setBucketLifecycle(resource.getExternalId(), input.toLifecycleConfiguration());
        } else {
            obsService.deleteBucketLifecycle(resource.getExternalId());
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
        HuaweiBucketUpdateLifecycleInput input = JSON.convert(parameters, HuaweiBucketUpdateLifecycleInput.class);
        input.validate();
    }
}
