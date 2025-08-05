package com.stratocloud.provider.tencent.cos.bucket.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.qcloud.cos.model.BucketCrossOriginConfiguration;
import com.qcloud.cos.model.CORSRule;
import com.stratocloud.form.*;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.utils.Utils;
import lombok.Data;

import java.util.List;

@Data
public class TencentBucketUpdateCorsInput implements ResourceActionInput {
    @BooleanField(label = "开启CORS规则")
    private boolean enabled;

    @NestedFormField(label = "CORS规则", multiple = true, nestedFormClass = RuleInput.class, conditions = "this.enabled === true")
    private List<RuleInput> rules;

    @Data
    public static class RuleInput implements DynamicForm {
        @InputField(label = "规则名称")
        private String ruleId;

        @SelectField(label = "来源Origin", multiSelect = true, allowCreate = true)
        private List<String> allowedOrigins;

        @SelectField(
                label = "操作Methods",
                multiSelect = true,
                allowCreate = true,
                options = {
                        "GET",
                        "PUT",
                        "HEAD",
                        "POST",
                        "DELETE"
                },
                optionNames = {
                        "GET",
                        "PUT",
                        "HEAD",
                        "POST",
                        "DELETE"
                }
        )
        private List<CORSRule.AllowedMethods> allowedMethods;

        @SelectField(
                label = "Allowed Headers",
                multiSelect = true,
                allowCreate = true,
                defaultValues = "*",
                options = {
                        "*"
                },
                optionNames = {
                        "*"
                },
                required = false
        )
        private List<String> allowedHeaders;

        @SelectField(
                label = "Exposed Headers",
                multiSelect = true,
                allowCreate = true,
                defaultValues = {
                        "ETag",
                        "Content-Length",
                        "x-cos-request-id"
                },
                options = {
                        "ETag",
                        "Content-Length",
                        "x-cos-request-id"
                },
                optionNames = {
                        "ETag",
                        "Content-Length",
                        "x-cos-request-id"
                },
                required = false
        )
        private List<String> exposedHeaders;

        @NumberField(label = "超时Max-Age (秒)", defaultValue = 600)
        private int maxAgeSeconds;


        public static RuleInput fromRule(CORSRule rule){
            RuleInput ruleInput = new RuleInput();
            ruleInput.setRuleId(rule.getId());
            ruleInput.setAllowedOrigins(rule.getAllowedOrigins());
            ruleInput.setAllowedMethods(rule.getAllowedMethods());
            ruleInput.setAllowedHeaders(rule.getAllowedHeaders());
            ruleInput.setExposedHeaders(rule.getExposedHeaders());
            ruleInput.setMaxAgeSeconds(rule.getMaxAgeSeconds());
            return ruleInput;
        }

        @JsonIgnore
        public CORSRule toRule(){
            CORSRule corsRule = new CORSRule();
            corsRule.setId(ruleId);

            corsRule.setAllowedMethods(allowedMethods);
            corsRule.setAllowedOrigins(allowedOrigins);
            corsRule.setExposedHeaders(exposedHeaders);
            corsRule.setAllowedHeaders(allowedHeaders);

            corsRule.setMaxAgeSeconds(maxAgeSeconds);

            return corsRule;
        }
    }

    public static TencentBucketUpdateCorsInput fromConfig(BucketCrossOriginConfiguration configuration){
        TencentBucketUpdateCorsInput input = new TencentBucketUpdateCorsInput();

        if(Utils.isNotEmpty(configuration.getRules())){
            input.setEnabled(true);
            input.setRules(
                    configuration.getRules().stream().map(
                            RuleInput::fromRule
                    ).toList()
            );
        } else {
            input.setEnabled(false);
        }

        return input;
    }

    @JsonIgnore
    public BucketCrossOriginConfiguration toConfig() {
        BucketCrossOriginConfiguration configuration = new BucketCrossOriginConfiguration();

        if(enabled && Utils.isNotEmpty(rules)){
            configuration.setRules(
                    rules.stream().map(
                            RuleInput::toRule
                    ).toList()
            );
        }

        return configuration;
    }
}
