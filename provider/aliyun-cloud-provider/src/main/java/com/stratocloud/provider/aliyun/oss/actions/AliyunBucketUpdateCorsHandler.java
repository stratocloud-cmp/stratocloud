package com.stratocloud.provider.aliyun.oss.actions;

import com.aliyun.oss.model.CORSConfiguration;
import com.aliyun.oss.model.SetBucketCORSRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.services.AliyunOssService;
import com.stratocloud.provider.aliyun.oss.AliyunBucketHandler;
import com.stratocloud.provider.constants.BucketActions;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class AliyunBucketUpdateCorsHandler implements ResourceActionHandler {

    private final AliyunBucketHandler bucketHandler;

    public AliyunBucketUpdateCorsHandler(AliyunBucketHandler bucketHandler) {
        this.bucketHandler = bucketHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return bucketHandler;
    }

    @Override
    public ResourceAction getAction() {
        return BucketActions.UPDATE_CORS;
    }

    @Override
    public String getTaskName() {
        return "配置存储桶CORS";
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
        return AliyunBucketUpdateCorsInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        if(Utils.isBlank(resource.getExternalId()))
            return Optional.empty();

        AliyunCloudProvider provider = (AliyunCloudProvider) bucketHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunOssService ossService = provider.buildClient(account).oss();

        Optional<CORSConfiguration> cors = ossService.describeBucketCors(resource.getExternalId());

        AliyunBucketUpdateCorsInput input = new AliyunBucketUpdateCorsInput();

        if(cors.isPresent()){
            input.setEnabled(true);
            input.setRules(convertToRuleInputs(cors.get().getCorsRules()));
        } else {
            input.setEnabled(false);
        }

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(AliyunBucketUpdateCorsInput.class);

        formMetaData = DynamicFormHelper.changeDefaultValues(formMetaData, input);

        return Optional.of(formMetaData);
    }

    private List<AliyunBucketUpdateCorsInput.Rule> convertToRuleInputs(List<SetBucketCORSRequest.CORSRule> corsRules) {
        List<AliyunBucketUpdateCorsInput.Rule> result = new ArrayList<>();

        if(Utils.isNotEmpty(corsRules)){
            for (SetBucketCORSRequest.CORSRule corsRule : corsRules) {
                AliyunBucketUpdateCorsInput.Rule rule = new AliyunBucketUpdateCorsInput.Rule();
                rule.setAllowedOrigins(corsRule.getAllowedOrigins());
                rule.setAllowedMethods(corsRule.getAllowedMethods());
                rule.setAllowedHeaders(corsRule.getAllowedHeaders());
                rule.setExposeHeaders(corsRule.getExposeHeaders());
                rule.setMaxAgeSeconds(corsRule.getMaxAgeSeconds());
                result.add(rule);
            }
        }

        return result;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        AliyunBucketUpdateCorsInput input = JSON.convert(parameters, AliyunBucketUpdateCorsInput.class);

        AliyunCloudProvider provider = (AliyunCloudProvider) bucketHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunOssService ossService = provider.buildClient(account).oss();

        if(input.isEnabled() && Utils.isNotEmpty(input.getRules())){
            ossService.setBucketCors(resource.getExternalId(), convertToRules(input.getRules()));
        } else {
            ossService.deleteBucketCors(resource.getExternalId());
        }
    }

    private List<SetBucketCORSRequest.CORSRule> convertToRules(List<AliyunBucketUpdateCorsInput.Rule> rules) {
        List<SetBucketCORSRequest.CORSRule> result = new ArrayList<>();

        if(Utils.isNotEmpty(rules)){
            for (AliyunBucketUpdateCorsInput.Rule rule : rules) {
                SetBucketCORSRequest.CORSRule corsRule = new SetBucketCORSRequest.CORSRule();
                corsRule.setAllowedOrigins(rule.getAllowedOrigins());
                corsRule.setAllowedMethods(rule.getAllowedMethods());
                corsRule.setAllowedHeaders(rule.getAllowedHeaders());
                corsRule.setExposeHeaders(rule.getExposeHeaders());
                corsRule.setMaxAgeSeconds(rule.getMaxAgeSeconds());
                result.add(corsRule);
            }
        }

        return result;
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
