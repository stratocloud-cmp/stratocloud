package com.stratocloud.provider.huawei.obs.actions;

import com.obs.services.model.ObsBucket;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.constants.BucketActions;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.common.HuaweiCloudClient;
import com.stratocloud.provider.huawei.common.services.HuaweiObsService;
import com.stratocloud.provider.huawei.obs.HuaweiBucketHandler;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public class HuaweiBucketUpdateLoggingHandler implements ResourceActionHandler {

    private final HuaweiBucketHandler bucketHandler;

    public HuaweiBucketUpdateLoggingHandler(HuaweiBucketHandler bucketHandler) {
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
        return HuaweiBucketUpdateLoggingInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        if(Utils.isBlank(resource.getExternalId()))
            return Optional.empty();

        HuaweiCloudProvider provider = (HuaweiCloudProvider) bucketHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudClient client = provider.buildClient(account);
        HuaweiObsService obsService = client.obs();

        var logging = obsService.describeBucketLogging(resource.getExternalId());

        HuaweiBucketUpdateLoggingInput input = new HuaweiBucketUpdateLoggingInput();

        if(logging.isPresent() && Utils.isNotBlank(input.getTargetBucket())){
            input.setEnabled(true);
            input.setTargetBucket(logging.get().getTargetBucketName());
            input.setTargetPrefix(logging.get().getLogfilePrefix());
            input.setAgency(logging.get().getAgency());
        }else {
            input.setEnabled(false);
        }

        List<String> bucketNames = obsService.describeBuckets().stream().map(ObsBucket::getBucketName).toList();

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(HuaweiBucketUpdateLoggingInput.class);

        formMetaData = DynamicFormHelper.changeDefaultValues(formMetaData, input);

        formMetaData = DynamicFormHelper.changeOptions(formMetaData, "targetBucket", bucketNames, bucketNames);

        return Optional.of(formMetaData);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        HuaweiBucketUpdateLoggingInput input = JSON.convert(parameters, HuaweiBucketUpdateLoggingInput.class);

        HuaweiCloudProvider provider = (HuaweiCloudProvider) bucketHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiObsService obsService = provider.buildClient(account).obs();

        if(input.isEnabled()){
            obsService.setBucketLogging(
                    resource.getExternalId(),
                    input.getTargetBucket(),
                    input.getTargetPrefix(),
                    input.getAgency()
            );
        } else {
            obsService.deleteBucketLogging(resource.getExternalId());
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
