package com.stratocloud.provider.tencent.cos.bucket.actions;

import com.qcloud.cos.model.RedirectRule;
import com.qcloud.cos.model.RoutingRule;
import com.stratocloud.form.*;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.cos.session.CosSession;
import com.stratocloud.provider.tencent.cos.session.CosSessionKey;
import com.stratocloud.provider.tencent.cos.session.CosSessionManager;
import com.stratocloud.resource.Resource;
import com.stratocloud.utils.Utils;
import lombok.Data;

import java.util.List;
import java.util.Objects;

@Data
public class TencentBucketUpdateWebsiteInput implements ResourceActionInput {
    @BooleanField(label = "启用静态网站", defaultValue = true)
    private boolean enabled;

    @InputField(label = "访问节点", required = false, disabled = true, conditions = "this.enabled === true")
    private String websiteDomainName;

    @InputField(label = "索引文档", defaultValue = "index.html", conditions = "this.enabled === true")
    private String indexDocumentSuffix;
    @InputField(label = "错误文档", defaultValue = "error.html", conditions = "this.enabled === true")
    private String errorDocument;

    @BooleanField(label = "强制HTTPS", conditions = "this.enabled === true")
    private boolean forceHttps;

    @NestedFormField(
            label = "重定向规则",
            nestedFormClass = RoutingRuleInput.class,
            conditions = "this.enabled === true",
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

    public static TencentBucketUpdateWebsiteInput getInput(Resource bucketResource){
        var provider = (TencentCloudProvider) bucketResource.getResourceHandler().getProvider();
        var account = provider.getAccountRepository().findExternalAccount(bucketResource.getAccountId());
        TencentCloudClient client = provider.buildClient(account);
        CosSessionKey sessionKey = client.getCosSessionKey();
        CosSession cosSession = CosSessionManager.getSession(sessionKey);
        var website = cosSession.describeBucketWebsite(
                bucketResource.getExternalId()
        );

        TencentBucketUpdateWebsiteInput t = new TencentBucketUpdateWebsiteInput();

        t.setWebsiteDomainName(
                "https://%s.cos-website.%s.myqcloud.com".formatted(
                        bucketResource.getExternalId(),
                        client.getRegion()
                )
        );

        if(website.isPresent()){
            t.setEnabled(true);
            t.setIndexDocumentSuffix(website.get().getIndexDocumentSuffix());
            t.setErrorDocument(website.get().getErrorDocument());
            t.setRoutingRules(convertRoutingRules(website.get().getRoutingRules()));

            RedirectRule allRequestsTo = website.get().getRedirectAllRequestsTo();
            if(allRequestsTo != null && Objects.equals(allRequestsTo.getprotocol(), "https"))
                t.setForceHttps(true);
        } else {
            t.setEnabled(false);
        }

        return t;
    }

    private static List<RoutingRuleInput> convertRoutingRules(List<RoutingRule> routingRules) {
        if(Utils.isEmpty(routingRules))
            return null;
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
                            Objects.equals(routingRule.getRedirect().getprotocol(), "https")
                    );

                    return ruleInput;
                }
        ).toList();
    }
}
