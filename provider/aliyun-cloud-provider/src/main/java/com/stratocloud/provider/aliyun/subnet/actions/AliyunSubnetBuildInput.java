package com.stratocloud.provider.aliyun.subnet.actions;

import com.stratocloud.constant.RegexExpressions;
import com.stratocloud.form.BooleanField;
import com.stratocloud.form.InputField;
import com.stratocloud.form.NumberField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class AliyunSubnetBuildInput implements ResourceActionInput {
    @InputField(
            label = "子网的网段(CIDR)",
            regex = RegexExpressions.IPV4_CIDR_REGEX,
            regexMessage = "CIDR格式不正确",
            description = "子网网段，子网网段必须在VPC网段内，相同VPC内子网网段不能重叠。"
    )
    private String cidrBlock;

    @BooleanField(label = "开启IPv6网段")
    private Boolean enableIpv6;

    @NumberField(
            label = "子网的IPv6网段(CIDR)",
            description = "交换机 IPv6 网段的最后 8 比特位，取值：0～255。",
            required = false,
            conditions = "this.enableIpv6 === true"
    )
    private Integer ipv6CidrBlock;
}
