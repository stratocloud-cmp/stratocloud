package com.stratocloud.provider.tencent.cos.cors.actions;

import com.qcloud.cos.model.CORSRule;
import com.stratocloud.form.InputField;
import com.stratocloud.form.NumberField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

import java.util.List;

@Data
public class TencentBucketCorsRuleBuildInput implements ResourceActionInput {
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
            }
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
            }
    )
    private List<String> exposedHeaders;

    @NumberField(label = "超时Max-Age (秒)", defaultValue = 600)
    private int maxAgeSeconds;
}
