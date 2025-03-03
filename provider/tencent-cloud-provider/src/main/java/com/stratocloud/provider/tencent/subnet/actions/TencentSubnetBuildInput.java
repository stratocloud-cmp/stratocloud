package com.stratocloud.provider.tencent.subnet.actions;

import com.stratocloud.constant.RegexExpressions;
import com.stratocloud.form.InputField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class TencentSubnetBuildInput implements ResourceActionInput {
    @InputField(
            label = "子网CIDR",
            regex = RegexExpressions.IPV4_CIDR_REGEX,
            regexMessage = "CIDR格式不正确",
            description = "子网网段，子网网段必须在VPC网段内，相同VPC内子网网段不能重叠。"
    )
    private String cidrBlock;
}
