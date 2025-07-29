package com.stratocloud.provider.tencent.cos.bucket.actions;

import com.stratocloud.form.BooleanField;
import com.stratocloud.form.CodeBlockField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class TencentBucketUpdatePolicyInput implements ResourceActionInput {
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
              "Statement": [
                {
                  "Principal": {
                    "qcs": [
                      "qcs::cam::uin/100000000001:uin/100000000011"
                    ]
                  },
                  "Effect": "allow",
                  "Action": [
                    "name/cos:GetBucket"
                  ],
                  "Resource": [
                    "qcs::cos:ap-guangzhou:uid/1250000000:examplebucket-1250000000/*"
                  ]
                }
              ],
              "version": "2.0"
            }
            """;
}
