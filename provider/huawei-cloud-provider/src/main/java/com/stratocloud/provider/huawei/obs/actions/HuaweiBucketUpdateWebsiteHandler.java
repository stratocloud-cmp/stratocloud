package com.stratocloud.provider.huawei.obs.actions;

import com.obs.services.model.*;
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
public class HuaweiBucketUpdateWebsiteHandler implements ResourceActionHandler {

    private final HuaweiBucketHandler bucketHandler;

    public HuaweiBucketUpdateWebsiteHandler(HuaweiBucketHandler bucketHandler) {
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
        return HuaweiBucketUpdateWebsiteInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        if(Utils.isBlank(resource.getExternalId()))
            return Optional.empty();

        HuaweiBucketUpdateWebsiteInput input = HuaweiBucketUpdateWebsiteInput.getInput(resource);
        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(HuaweiBucketUpdateWebsiteInput.class);
        formMetaData = DynamicFormHelper.changeDefaultValues(formMetaData, input);
        return Optional.of(formMetaData);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        HuaweiBucketUpdateWebsiteInput input = JSON.convert(parameters, HuaweiBucketUpdateWebsiteInput.class);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) bucketHandler.getProvider();
        HuaweiObsService obsService = provider.buildClient(account).obs();

        if(input.isEnabled()){
            WebsiteConfiguration configuration = new WebsiteConfiguration();

            if(input.isRedirectAllRequests()){
                RedirectAllRequest redirectAllRequest = new RedirectAllRequest();
                redirectAllRequest.setRedirectProtocol(input.isForceHttps() ? ProtocolEnum.HTTPS : ProtocolEnum.HTTP);
                redirectAllRequest.setHostName(input.getRedirectToHostName());
                configuration.setRedirectAllRequestsTo(redirectAllRequest);
            }else {
                configuration.setSuffix(input.getIndexDocumentSuffix());
                configuration.setKey(input.getErrorDocument());
                configuration.setRouteRules(convertRoutingRules(input.getRoutingRules()));
            }

            obsService.setBucketWebsite(new SetBucketWebsiteRequest(resource.getExternalId(), configuration));
        }else {
            obsService.deleteBucketWebsite(resource.getExternalId());
        }
    }

    private List<RouteRule> convertRoutingRules(List<HuaweiBucketUpdateWebsiteInput.RoutingRuleInput> routingRules) {
        List<RouteRule> result = new ArrayList<>();

        if(Utils.isEmpty(routingRules))
            return result;

        for (HuaweiBucketUpdateWebsiteInput.RoutingRuleInput routingRuleInput : routingRules) {
            RouteRule routingRule = new RouteRule();

            RouteRuleCondition condition = new RouteRuleCondition();
            routingRule.setCondition(condition);

            Redirect redirect = new Redirect();
            routingRule.setRedirect(redirect);

            if(routingRuleInput.getRoutingType() == HuaweiBucketUpdateWebsiteInput.RoutingType.KeyPrefix){
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
                redirect.setRedirectProtocol(ProtocolEnum.HTTPS);

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
