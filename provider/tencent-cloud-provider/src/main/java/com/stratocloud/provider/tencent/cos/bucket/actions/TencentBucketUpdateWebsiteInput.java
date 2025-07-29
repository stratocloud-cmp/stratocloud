package com.stratocloud.provider.tencent.cos.bucket.actions;

import com.qcloud.cos.model.RoutingRule;
import com.stratocloud.form.BooleanField;
import com.stratocloud.form.DynamicForm;
import com.stratocloud.form.InputField;
import com.stratocloud.form.NestedFormField;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.cos.session.CosSession;
import com.stratocloud.provider.tencent.cos.session.CosSessionKey;
import com.stratocloud.provider.tencent.cos.session.CosSessionManager;
import com.stratocloud.resource.Resource;
import com.stratocloud.utils.Utils;
import lombok.Data;

import java.util.List;

@Data
public class TencentBucketUpdateWebsiteInput implements ResourceActionInput {
    @BooleanField(label = "启用静态网站", defaultValue = true)
    private boolean enabled;

    @InputField(label = "索引文档", defaultValue = "index.html", conditions = "this.enabled === true")
    private String indexDocumentSuffix;
    @InputField(label = "错误文档", defaultValue = "error.html", conditions = "this.enabled === true")
    private String errorDocument;

    @NestedFormField(
            label = "重定向规则",
            nestedFormClass = RoutingRuleInput.class,
            conditions = "this.enabled === true",
            multiple = true
    )
    private List<RoutingRuleInput> routingRules;

    @Data
    public static class RoutingRuleInput implements DynamicForm {
        @InputField(label = "keyPrefixEquals", required = false)
        private String keyPrefixEquals;
        @InputField(label = "httpErrorCodeReturnedEquals", required = false)
        private String httpErrorCodeReturnedEquals;
        @InputField(label = "protocol", required = false)
        private String protocol;
        @InputField(label = "hostName", required = false)
        private String hostName;
        @InputField(label = "replaceKeyPrefixWith", required = false)
        private String replaceKeyPrefixWith;
        @InputField(label = "replaceKeyWith", required = false)
        private String replaceKeyWith;
        @InputField(label = "httpRedirectCode", required = false)
        private String httpRedirectCode;
    }

    public static TencentBucketUpdateWebsiteInput getInput(Resource bucketResource){
        var provider = (TencentCloudProvider) bucketResource.getResourceHandler().getProvider();
        var account = provider.getAccountRepository().findExternalAccount(bucketResource.getAccountId());
        CosSessionKey sessionKey = provider.buildClient(account).getCosSessionKey();
        CosSession cosSession = CosSessionManager.getSession(sessionKey);
        var website = cosSession.describeBucketWebsite(
                bucketResource.getExternalId()
        );

        TencentBucketUpdateWebsiteInput t = new TencentBucketUpdateWebsiteInput();

        if(website.isPresent()){
            t.setEnabled(true);
            t.setIndexDocumentSuffix(website.get().getIndexDocumentSuffix());
            t.setErrorDocument(website.get().getErrorDocument());
            t.setRoutingRules(convertRoutingRules(website.get().getRoutingRules()));
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
                    ruleInput.setKeyPrefixEquals(routingRule.getCondition().getKeyPrefixEquals());
                    ruleInput.setHttpErrorCodeReturnedEquals(
                            routingRule.getCondition().getHttpErrorCodeReturnedEquals()
                    );
                    ruleInput.setProtocol(routingRule.getRedirect().getprotocol());
                    ruleInput.setHostName(routingRule.getRedirect().getHostName());
                    ruleInput.setReplaceKeyPrefixWith(
                            routingRule.getRedirect().getReplaceKeyPrefixWith()
                    );
                    ruleInput.setReplaceKeyWith(routingRule.getRedirect().getReplaceKeyWith());
                    ruleInput.setHttpRedirectCode(routingRule.getRedirect().getHttpRedirectCode());
                    return ruleInput;
                }
        ).toList();
    }
}
