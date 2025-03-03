package com.stratocloud.provider.aliyun.securitygroup.actions;

import com.stratocloud.form.SelectField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class AliyunSecurityGroupBuildInput implements ResourceActionInput {
    @SelectField(
            label = "安全组类型",
            options = {"normal", "enterprise"},
            optionNames = {"普通安全组", "企业安全组"},
            defaultValues = "normal"
    )
    private String securityGroupType;

}
