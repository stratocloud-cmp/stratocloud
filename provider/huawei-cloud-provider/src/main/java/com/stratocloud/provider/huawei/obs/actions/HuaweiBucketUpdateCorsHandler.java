package com.stratocloud.provider.huawei.obs.actions;

import com.obs.services.model.BucketCors;
import com.obs.services.model.BucketCorsRule;
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

import java.util.*;

@Component
public class HuaweiBucketUpdateCorsHandler implements ResourceActionHandler {

    private final HuaweiBucketHandler bucketHandler;

    public HuaweiBucketUpdateCorsHandler(HuaweiBucketHandler bucketHandler) {
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
        return HuaweiBucketUpdateCorsInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        if(Utils.isBlank(resource.getExternalId()))
            return Optional.empty();

        HuaweiCloudProvider provider = (HuaweiCloudProvider) bucketHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiObsService obsService = provider.buildClient(account).obs();

        Optional<BucketCors> cors = obsService.describeBucketCors(resource.getExternalId());

        HuaweiBucketUpdateCorsInput input = new HuaweiBucketUpdateCorsInput();

        if(cors.isPresent()){
            input.setEnabled(true);
            input.setRules(convertToRuleInputs(cors.get().getRules()));
        } else {
            input.setEnabled(false);
        }

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(HuaweiBucketUpdateCorsInput.class);

        formMetaData = DynamicFormHelper.changeDefaultValues(formMetaData, input);

        return Optional.of(formMetaData);
    }

    private List<HuaweiBucketUpdateCorsInput.Rule> convertToRuleInputs(List<BucketCorsRule> corsRules) {
        List<HuaweiBucketUpdateCorsInput.Rule> result = new ArrayList<>();

        if(Utils.isNotEmpty(corsRules)){
            for (BucketCorsRule corsRule : corsRules) {
                HuaweiBucketUpdateCorsInput.Rule rule = new HuaweiBucketUpdateCorsInput.Rule();
                rule.setAllowedOrigins(corsRule.getAllowedHeader());
                rule.setAllowedMethods(corsRule.getAllowedMethod());
                rule.setAllowedHeaders(corsRule.getAllowedHeader());
                rule.setExposeHeaders(corsRule.getExposeHeader());
                rule.setMaxAgeSeconds(corsRule.getMaxAgeSecond());
                result.add(rule);
            }
        }

        return result;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        HuaweiBucketUpdateCorsInput input = JSON.convert(parameters, HuaweiBucketUpdateCorsInput.class);

        HuaweiCloudProvider provider = (HuaweiCloudProvider) bucketHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiObsService obsService = provider.buildClient(account).obs();

        if(input.isEnabled() && Utils.isNotEmpty(input.getRules())){
            obsService.setBucketCors(resource.getExternalId(), new BucketCors(convertToRules(input.getRules())));
        } else {
            obsService.deleteBucketCors(resource.getExternalId());
        }
    }

    private List<BucketCorsRule> convertToRules(List<HuaweiBucketUpdateCorsInput.Rule> rules) {
        List<BucketCorsRule> result = new ArrayList<>();

        if(Utils.isNotEmpty(rules)){
            for (HuaweiBucketUpdateCorsInput.Rule rule : rules) {
                BucketCorsRule corsRule = new BucketCorsRule();
                corsRule.setAllowedOrigin(rule.getAllowedOrigins());
                corsRule.setAllowedMethod(rule.getAllowedMethods());
                corsRule.setAllowedHeader(rule.getAllowedHeaders());
                corsRule.setExposeHeader(rule.getExposeHeaders());
                corsRule.setMaxAgeSecond(rule.getMaxAgeSeconds());
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
