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
public class HuaweiBucketUpdatePolicyHandler implements ResourceActionHandler {

    private final HuaweiBucketHandler bucketHandler;

    public HuaweiBucketUpdatePolicyHandler(HuaweiBucketHandler bucketHandler) {
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

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(HuaweiBucketUpdatePolicyInput.class);
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) bucketHandler.getProvider();
        HuaweiObsService obsService = provider.buildClient(account).obs();

        var policy = obsService.describeBucketPolicy(resource.getExternalId());

        HuaweiBucketUpdatePolicyInput input = new HuaweiBucketUpdatePolicyInput();
        if(policy.isEmpty()) {
            input.setEnabled(false);
        } else {
            input.setEnabled(true);
            input.setPolicyText(policy.get().getPolicy());
        }

        formMetaData = DynamicFormHelper.changeDefaultValues(
                formMetaData,
                input
        );

        return Optional.of(formMetaData);
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return HuaweiBucketUpdatePolicyInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        HuaweiBucketUpdatePolicyInput input = JSON.convert(parameters, HuaweiBucketUpdatePolicyInput.class);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) bucketHandler.getProvider();
        HuaweiObsService obsService = provider.buildClient(account).obs();

        if(input.isEnabled())
            obsService.setBucketPolicy(resource.getExternalId(), input.getPolicyText());
        else
            obsService.deleteBucketPolicy(resource.getExternalId());
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
