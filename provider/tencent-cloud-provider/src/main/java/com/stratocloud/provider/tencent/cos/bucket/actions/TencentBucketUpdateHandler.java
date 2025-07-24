package com.stratocloud.provider.tencent.cos.bucket.actions;

import com.qcloud.cos.model.Bucket;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.SelectEntityType;
import com.stratocloud.form.SelectType;
import com.stratocloud.form.Source;
import com.stratocloud.form.info.BooleanFieldDetail;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.form.info.InputFieldDetail;
import com.stratocloud.form.info.SelectFieldDetail;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.cos.bucket.TencentBucketHandler;
import com.stratocloud.provider.tencent.cos.bucket.TencentBucketSpec;
import com.stratocloud.provider.tencent.cos.session.CosSession;
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
public class TencentBucketUpdateHandler implements ResourceActionHandler {

    private final TencentBucketHandler bucketHandler;

    public TencentBucketUpdateHandler(TencentBucketHandler bucketHandler) {
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
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) bucketHandler.getProvider();
        CosSession cosSession = CosSessionManager.getSession(provider.buildClient(account).getCosSessionKey());

        TencentBucketSpec bucketSpec = TencentBucketSpec.retrieveFrom(cosSession, resource);

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(TencentBucketUpdateInput.class);

        formMetaData = DynamicFormHelper.changeFieldDetail(
                formMetaData,
                "enableVersioning",
                new BooleanFieldDetail(
                        bucketSpec.isEnableVersioning(),
                        List.of()
                )
        );

        formMetaData = DynamicFormHelper.changeFieldDetail(
                formMetaData,
                "enableIntelligentTier",
                new BooleanFieldDetail(
                        bucketSpec.isEnableIntelligentTier(),
                        List.of()
                )
        );

        SelectFieldDetail defaultIntelligentTierDaysFieldTDetail = new SelectFieldDetail(
                false,
                false,
                List.of(String.valueOf(bucketSpec.getDefaultIntelligentTierDays())),
                List.of("30", "60", "90"),
                List.of("30", "60", "90"),
                Source.STATIC,
                SelectEntityType.NONE,
                List.of(),
                true,
                List.of("this.enableIntelligentTier === true"),
                SelectType.NORMAL
        );
        formMetaData = DynamicFormHelper.changeFieldDetail(
                formMetaData,
                "defaultIntelligentTierDays",
                defaultIntelligentTierDaysFieldTDetail
        );

        formMetaData = DynamicFormHelper.changeFieldDetail(
                formMetaData,
                "enableLogging",
                new BooleanFieldDetail(
                        bucketSpec.isEnableLogging(),
                        List.of()
                )
        );

        List<String> bucketNames = cosSession.describeBuckets().stream().map(Bucket::getName).toList();
        String targetBucketName = bucketSpec.getLoggingTargetBucketName();
        SelectFieldDetail loggingTargetBucketFieldDetail = new SelectFieldDetail(
                false,
                false,
                Utils.isNotBlank(targetBucketName) ? List.of(targetBucketName) : List.of(),
                bucketNames,
                bucketNames,
                Source.STATIC,
                SelectEntityType.NONE,
                List.of(),
                true,
                List.of("this.enableLogging === true"),
                SelectType.NORMAL
        );
        formMetaData = DynamicFormHelper.changeFieldDetail(
                formMetaData,
                "loggingTargetBucketName",
                loggingTargetBucketFieldDetail
        );

        formMetaData = DynamicFormHelper.changeFieldDetail(
                formMetaData,
                "loggingFilePrefix",
                new InputFieldDetail(
                        bucketSpec.getLoggingFilePrefix(),
                        true,
                        List.of("this.enableLogging === true"),
                        "",
                        "",
                        "text"
                )
        );

        return Optional.of(formMetaData);
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return TencentBucketUpdateInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) bucketHandler.getProvider();
        CosSession cosSession = CosSessionManager.getSession(provider.buildClient(account).getCosSessionKey());

        Bucket bucket = bucketHandler.describeBucket(account, resource.getExternalId()).orElseThrow(
                () -> new StratoException("Bucket not found")
        );

        TencentBucketUpdateInput input = JSON.convert(parameters, TencentBucketUpdateInput.class);

        TencentBucketSpec bucketSpec = input.toSpec();

        bucketSpec.applyVersioningQuietly(cosSession, bucket.getName());
        bucketSpec.applyIntelligentTierQuietly(cosSession, bucket.getName());
        bucketSpec.applyLoggingQuietly(cosSession, bucket.getName());
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
        if(Utils.isBlank(resource.getExternalId()))
            return;

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) bucketHandler.getProvider();
        CosSession cosSession = CosSessionManager.getSession(provider.buildClient(account).getCosSessionKey());

        if(!cosSession.doesBucketExist(resource.getExternalId()))
            throw new BadCommandException("存储桶不存在");

        TencentBucketUpdateInput input = JSON.convert(parameters, TencentBucketUpdateInput.class);

        TencentBucketSpec currentSpec = TencentBucketSpec.retrieveFrom(cosSession, resource);
        TencentBucketSpec newSpec = input.toSpec();

        if(currentSpec.isEnableIntelligentTier() && !newSpec.isEnableVersioning())
            throw new BadCommandException("智能分层开启后无法关闭");
    }
}
