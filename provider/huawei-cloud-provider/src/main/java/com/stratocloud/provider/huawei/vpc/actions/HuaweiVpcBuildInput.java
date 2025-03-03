package com.stratocloud.provider.huawei.vpc.actions;

import com.stratocloud.constant.RegexExpressions;
import com.stratocloud.form.InputField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class HuaweiVpcBuildInput implements ResourceActionInput {
    @InputField(
            label = "CIDR",
            regex = RegexExpressions.IPV4_CIDR_REGEX,
            regexMessage = "CIDR格式不正确",
            description = "仅能在10.0.0.0/8~24，172.16.0.0/12~24，192.168.0.0/16~24这三个内网网段内。示例值：10.8.0.0/16"
    )
    private String cidr;
}
