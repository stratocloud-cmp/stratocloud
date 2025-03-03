package com.stratocloud.provider.huawei.keypair.actions;

import com.stratocloud.form.InputField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class HuaweiKeyPairBuildInput implements ResourceActionInput {
    @SelectField(
            label = "类型",
            options = {
                    "ssh", "x509"
            },
            optionNames = {
                    "SSH", "X.509"
            },
            defaultValues = "ssh"
    )
    private String type;
    @InputField(label = "公钥", inputType = "textarea", required = false)
    private String publicKey;
}
