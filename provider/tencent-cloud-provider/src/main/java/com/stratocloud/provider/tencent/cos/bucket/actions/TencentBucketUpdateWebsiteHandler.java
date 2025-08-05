package com.stratocloud.provider.tencent.cos.bucket.actions;

import com.qcloud.cos.model.BucketWebsiteConfiguration;
import com.qcloud.cos.model.RedirectRule;
import com.qcloud.cos.model.RoutingRule;
import com.qcloud.cos.model.RoutingRuleCondition;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.constants.BucketActions;
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

import java.util.*;

@Component
public class TencentBucketUpdateWebsiteHandler implements ResourceActionHandler {

    private final TencentBucketHandler bucketHandler;

    public TencentBucketUpdateWebsiteHandler(TencentBucketHandler bucketHandler) {
        this.bucketHandler = bucketHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return bucketHandler;
    }

    @Override
    public ResourceAction getAction() {
        return BucketActions.UPDATE_WEBSITE;
    }

    @Override
    public String getTaskName() {
        return "配置存储桶静态网站";
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
        return TencentBucketUpdateWebsiteInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        if(Utils.isBlank(resource.getExternalId()))
            return Optional.empty();

        TencentBucketUpdateWebsiteInput input = TencentBucketUpdateWebsiteInput.getInput(resource);
        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(TencentBucketUpdateWebsiteInput.class);
        formMetaData = DynamicFormHelper.changeDefaultValues(formMetaData, input);
        return Optional.of(formMetaData);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        TencentBucketUpdateWebsiteInput input = JSON.convert(parameters, TencentBucketUpdateWebsiteInput.class);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) bucketHandler.getProvider();
        CosSessionKey sessionKey = provider.buildClient(account).getCosSessionKey();
        CosSession cosSession = CosSessionManager.getSession(sessionKey);

        if(input.isEnabled()){
            BucketWebsiteConfiguration configuration = new BucketWebsiteConfiguration();
            configuration.setIndexDocumentSuffix(input.getIndexDocumentSuffix());
            configuration.setErrorDocument(input.getErrorDocument());
            configuration.setRoutingRules(convertRoutingRules(input.getRoutingRules()));

            if(input.isForceHttps()){
                RedirectRule redirectAllRequestsTo = new RedirectRule();
                redirectAllRequestsTo.setProtocol("https");
                configuration.setRedirectAllRequestsTo(redirectAllRequestsTo);
            }

            cosSession.setBucketWebsite(resource.getExternalId(), configuration);
        }else {
            cosSession.deleteBucketWebsite(resource.getExternalId());
        }
    }

    private List<RoutingRule> convertRoutingRules(List<TencentBucketUpdateWebsiteInput.RoutingRuleInput> routingRules) {
        List<RoutingRule> result = new ArrayList<>();

        if(Utils.isEmpty(routingRules))
            return result;

        for (TencentBucketUpdateWebsiteInput.RoutingRuleInput routingRuleInput : routingRules) {
            RoutingRule routingRule = new RoutingRule();

            RoutingRuleCondition condition = new RoutingRuleCondition();
            routingRule.setCondition(condition);

            RedirectRule redirect = new RedirectRule();
            routingRule.setRedirect(redirect);

            if(routingRuleInput.getRoutingType() == TencentBucketUpdateWebsiteInput.RoutingType.KeyPrefix){
                condition.setKeyPrefixEquals(routingRuleInput.getKeyPrefix());

                if(routingRuleInput.isReplaceKeyPrefix()){
                    redirect.setReplaceKeyPrefixWith(routingRuleInput.getReplaceKeyPrefixWith());
                } else {
                    redirect.setReplaceKeyWith(routingRuleInput.getReplaceKeyWith());
                }
            } else {
                condition.setHttpErrorCodeReturnedEquals(routingRuleInput.getErrorCode());
                redirect.setReplaceKeyWith(routingRuleInput.getReplaceKeyWith());
            }

            if(routingRuleInput.isForceHttps())
                redirect.setProtocol("https");

            result.add(routingRule);
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
