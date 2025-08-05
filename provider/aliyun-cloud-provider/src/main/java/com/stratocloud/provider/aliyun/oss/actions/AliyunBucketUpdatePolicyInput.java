package com.stratocloud.provider.aliyun.oss.actions;

import com.stratocloud.form.BooleanField;
import com.stratocloud.form.CodeBlockField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class AliyunBucketUpdatePolicyInput implements ResourceActionInput {
    @BooleanField(label = "启用访问策略", defaultValue = true)
    private boolean enabled;

    @CodeBlockField(
            label = "访问策略JSON",
            language = "json",
            defaultValue = policyTextExample,
            conditions = "this.enabled === true"
    )
    private String policyText;

    public static final String policyTextExample = """
            {
               "Version":"1",
               "Statement":[
               {
                 "Action":[
                   "oss:PutObject",
                   "oss:GetObject"
                ],
                "Effect":"Deny",
                "Principal":["1234567890"],
                "Resource":["acs:oss:*:1234567890:*/*"]
               }
              ]
            }
            """;
}
