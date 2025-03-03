package com.stratocloud.provider.huawei.subnet.actions;

import com.stratocloud.constant.RegexExpressions;
import com.stratocloud.form.BooleanField;
import com.stratocloud.form.InputField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class HuaweiSubnetBuildInput implements ResourceActionInput {
    @InputField(
            label = "子网的网段(CIDR)",
            regex = RegexExpressions.IPV4_CIDR_REGEX,
            regexMessage = "CIDR格式不正确",
            description = "子网网段，子网网段必须在VPC网段内，相同VPC内子网网段不能重叠。"
    )
    private String cidr;

    @InputField(
            label = "子网的网关",
            regex = RegexExpressions.IP_REGEX,
            regexMessage = "IP格式不正确",
            description = "子网网段中的IP地址"
    )
    private String gatewayIp;

    @BooleanField(label = "开启IPv6网段")
    private boolean enableIpv6;
}
