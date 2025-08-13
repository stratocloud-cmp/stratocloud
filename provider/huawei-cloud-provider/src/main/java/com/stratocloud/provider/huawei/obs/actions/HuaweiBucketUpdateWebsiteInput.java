package com.stratocloud.provider.huawei.obs.actions;

import com.obs.services.model.ProtocolEnum;
import com.obs.services.model.RedirectAllRequest;
import com.obs.services.model.RouteRule;
import com.stratocloud.form.*;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.common.HuaweiCloudClient;
import com.stratocloud.provider.huawei.common.services.HuaweiObsService;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.resource.Resource;
import com.stratocloud.utils.Utils;
import lombok.Data;

import java.util.List;

@Data
public class HuaweiBucketUpdateWebsiteInput implements ResourceActionInput {
    @BooleanField(label = "启用静态网站", defaultValue = true)
    private boolean enabled;

    @BooleanField(label = "重定向所有请求")
    private boolean redirectAllRequests;

    @BooleanField(label = "强制HTTPS", conditions = "this.enabled === true && this.redirectAllRequests === true")
    private boolean forceHttps;

    @InputField(label = "重定向至", conditions = "this.enabled === true && this.redirectAllRequests === true")
    private String redirectToHostName;

    @InputField(
            label = "索引文档",
            defaultValue = "index.html",
            conditions = "this.enabled === true && this.redirectAllRequests === false"
    )
    private String indexDocumentSuffix;
    @InputField(
            label = "错误文档",
            defaultValue = "error.html",
            conditions = "this.enabled === true && this.redirectAllRequests === false"
    )
    private String errorDocument;



    @NestedFormField(
            label = "重定向规则",
            nestedFormClass = RoutingRuleInput.class,
            conditions = "this.enabled === true && this.redirectAllRequests === false",
            multiple = true
    )
    private List<RoutingRuleInput> routingRules;

    public enum RoutingType {
        ErrorCode, KeyPrefix
    }

    @Data
    public static class RoutingRuleInput implements DynamicForm {
        @SelectField(
                label = "类型",
                options = {
                        "ErrorCode",
                        "KeyPrefix"
                },
                optionNames = {
                        "错误码",
                        "前缀匹配"
                },
                defaultValues = "ErrorCode"
        )
        private RoutingType routingType;

        @InputField(
                label = "错误码",
                description = "支持4xx错误码",
                conditions = "this.routingType === 'ErrorCode'"
        )
        private String errorCode;
        @InputField(
                label = "前缀",
                conditions = "this.routingType === 'KeyPrefix'"
        )
        private String keyPrefix;

        @BooleanField(label = "强制HTTPS")
        private boolean forceHttps;

        @BooleanField(
                label = "是否替换前缀",
                conditions = "this.routingType === 'KeyPrefix'"
        )
        private boolean replaceKeyPrefix;

        @InputField(
                label = "路径",
                conditions = "this.routingType === 'ErrorCode' || (this.routingType === 'KeyPrefix' && this.replaceKeyPrefix === false)"
        )
        private String replaceKeyWith;
        @InputField(
                label = "前缀",
                conditions = "this.routingType === 'KeyPrefix' && this.replaceKeyPrefix === true"
        )
        private String replaceKeyPrefixWith;
    }

    public static HuaweiBucketUpdateWebsiteInput getInput(Resource bucketResource){
        var provider = (HuaweiCloudProvider) bucketResource.getResourceHandler().getProvider();
        var account = provider.getAccountRepository().findExternalAccount(bucketResource.getAccountId());
        HuaweiCloudClient client = provider.buildClient(account);

        HuaweiObsService obsService = client.obs();

        var website = obsService.describeBucketWebsite(
                bucketResource.getExternalId()
        );

        HuaweiBucketUpdateWebsiteInput t = new HuaweiBucketUpdateWebsiteInput();

        if(website.isPresent()){
            t.setEnabled(true);

            RedirectAllRequest redirectAllRequest = website.get().getRedirectAllRequestsTo();

            if(redirectAllRequest != null){
                t.setRedirectToHostName(redirectAllRequest.getHostName());
                t.setForceHttps(redirectAllRequest.getRedirectProtocol() == ProtocolEnum.HTTPS);
            }else {
                t.setIndexDocumentSuffix(website.get().getSuffix());
                t.setErrorDocument(website.get().getKey());
                t.setRoutingRules(convertRoutingRules(website.get().getRouteRules()));
            }
        } else {
            t.setEnabled(false);
        }

        return t;
    }

    private static List<RoutingRuleInput> convertRoutingRules(List<RouteRule> routingRules) {
        if(Utils.isEmpty(routingRules))
            return List.of();
        return routingRules.stream().map(
                routingRule -> {
                    RoutingRuleInput ruleInput = new RoutingRuleInput();

                    if(Utils.isNotBlank(routingRule.getCondition().getHttpErrorCodeReturnedEquals())){
                        ruleInput.setRoutingType(RoutingType.ErrorCode);
                        ruleInput.setErrorCode(routingRule.getCondition().getHttpErrorCodeReturnedEquals());
                        ruleInput.setReplaceKeyWith(routingRule.getRedirect().getReplaceKeyWith());
                    }else {
                        ruleInput.setRoutingType(RoutingType.KeyPrefix);
                        ruleInput.setKeyPrefix(routingRule.getCondition().getKeyPrefixEquals());

                        if(Utils.isNotBlank(routingRule.getRedirect().getReplaceKeyPrefixWith())){
                            ruleInput.setReplaceKeyPrefix(true);
                            ruleInput.setReplaceKeyPrefixWith(routingRule.getRedirect().getReplaceKeyPrefixWith());
                        } else {
                            ruleInput.setReplaceKeyPrefix(false);
                            ruleInput.setReplaceKeyWith(routingRule.getRedirect().getReplaceKeyWith());
                        }
                    }

                    ruleInput.setForceHttps(
                            routingRule.getRedirect().getRedirectProtocol() == ProtocolEnum.HTTPS
                    );

                    return ruleInput;
                }
        ).toList();
    }
}
