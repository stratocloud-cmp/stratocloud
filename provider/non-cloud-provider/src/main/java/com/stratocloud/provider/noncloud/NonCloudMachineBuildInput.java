package com.stratocloud.provider.noncloud;

import com.stratocloud.form.BooleanField;
import com.stratocloud.form.InputField;
import com.stratocloud.form.NumberField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.resource.OsType;
import lombok.Data;

@Data
public class NonCloudMachineBuildInput implements ResourceActionInput {
    @InputField(label = "管理IP")
    private String ip;
    @NumberField(label = "管理端口", min = 1, max = 65535, defaultValue = 22)
    private Integer port;
    @SelectField(
            label = "操作系统类型",
            options = {
                    "Linux", "Windows"
            },
            optionNames = {
                    "Linux", "Windows"
            },
            defaultValues = "Linux"
    )
    private OsType osType;
    @InputField(label = "用户名", defaultValue = "root")
    private String username;
    @BooleanField(label = "使用密钥对", conditions = "this.osType==='Linux'")
    private Boolean useKeyPair;
    @InputField(
            label = "密码",
            inputType = "password",
            conditions = "this.useKeyPair !== true || this.osType==='Windows'"
    )
    private String password;
    @InputField(
            label = "公钥",
            inputType = "textarea",
            conditions = "this.useKeyPair === true && this.osType==='Linux'"
    )
    private String publicKey;
    @InputField(
            label = "私钥",
            inputType = "textarea",
            conditions = "this.useKeyPair === true && this.osType==='Linux'"
    )
    private String privateKey;
    @InputField(
            label = "密钥口令(Passphrase)",
            inputType = "password",
            conditions = "this.useKeyPair === true && this.osType==='Linux'",
            required = false
    )
    private String passphrase;
}
