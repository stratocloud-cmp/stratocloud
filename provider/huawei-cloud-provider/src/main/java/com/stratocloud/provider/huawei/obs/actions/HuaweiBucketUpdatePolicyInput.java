package com.stratocloud.provider.huawei.obs.actions;

import com.stratocloud.form.BooleanField;
import com.stratocloud.form.CodeBlockField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class HuaweiBucketUpdatePolicyInput implements ResourceActionInput {
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
                        "Sid": "Stmt1375240018061",
                        "Action": [
                            "GetBucketLogging"
                        ],
                        "Effect": "Allow",
                        "Resource": "logging.bucket",
                        "Principal": {
                            "ID": [
                                "domain/783fc6652cf246c096ea836694f71855:user/*"
                            ]
                        }
                    }
                ]
            }
            """;
}
