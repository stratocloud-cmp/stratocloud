package com.stratocloud.provider.tencent.cos.bucket.actions;

import com.qcloud.cos.model.BucketRefererConfiguration;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.cos.bucket.TencentBucketHandler;
import com.stratocloud.provider.tencent.cos.session.CosSession;
import com.stratocloud.provider.tencent.cos.session.CosSessionKey;
import com.stratocloud.provider.tencent.cos.session.CosSessionManager;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class TencentBucketUpdateRefererHandler implements ResourceActionHandler {

    private final TencentBucketHandler bucketHandler;

    public TencentBucketUpdateRefererHandler(TencentBucketHandler bucketHandler) {
        this.bucketHandler = bucketHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return bucketHandler;
    }

    @Override
    public ResourceAction getAction() {
        return new ResourceAction(
                "UPDATE_BUCKET_REFERER",
                "配置防盗链",
                502
        );
    }

    @Override
    public String getTaskName() {
        return "配置存储桶防盗链";
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
        return TencentBucketUpdateRefererInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        if(Utils.isBlank(resource.getExternalId()))
            return Optional.empty();

        TencentBucketUpdateRefererInput input = TencentBucketUpdateRefererInput.getInput(resource);
        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(TencentBucketUpdateRefererInput.class);
        formMetaData = DynamicFormHelper.changeDefaultValues(formMetaData, input);
        return Optional.of(formMetaData);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        TencentBucketUpdateRefererInput input = JSON.convert(parameters, TencentBucketUpdateRefererInput.class);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) bucketHandler.getProvider();
        CosSessionKey sessionKey = provider.buildClient(account).getCosSessionKey();
        CosSession cosSession = CosSessionManager.getSession(sessionKey);

        BucketRefererConfiguration configuration = new BucketRefererConfiguration();

        configuration.setStatus(input.getStatus());
        configuration.setRefererType(input.getRefererType());
        configuration.setDomainList(input.getDomainList());
        configuration.setEmptyReferConfiguration(input.getEmptyReferer());

        cosSession.setBucketReferer(resource.getExternalId(), configuration);
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
