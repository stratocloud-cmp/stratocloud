package com.stratocloud.provider.aliyun.vpc.actions;

import com.stratocloud.constant.RegexExpressions;
import com.stratocloud.form.BooleanField;
import com.stratocloud.form.InputField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.aliyun.common.AliyunDescriptions;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class AliyunVpcBuildInput implements ResourceActionInput {
    @InputField(
            label = "VPC的网段(CIDR)",
            regex = RegexExpressions.IPV4_CIDR_REGEX,
            regexMessage = "CIDR格式不正确",
            description = AliyunDescriptions.VPC_CIDR
    )
    private String cidrBlock;

    @BooleanField(label = "开启IPv6网段")
    private Boolean enableIpv6;

    @InputField(
            label = "VPC的IPv6网段(CIDR)",
            regex = RegexExpressions.IPV6_CIDR_REGEX,
            regexMessage = "CIDR格式不正确",
            description = "VPC的IPv6网段",
            conditions = "this.enableIpv6 === true"
    )
    private String ipv6CidrBlock;

    @SelectField(
            label = "IPv6地址段类型",
            options = {
                    "BGP", "ChinaMobile", "ChinaUnicom", "ChinaTelecom"
            },
            optionNames = {
                    "默认", "中国移动", "中国联通", "中国电信"
            },
            defaultValues = "BGP",
            conditions = "this.enableIpv6 === true"
    )
    private String ipv6Isp;

    @InputField(
            label = "用户网段",
            description = "用户网段，如需定义多个网段请使用半角逗号（,）隔开，最多支持 3 个网段。",
            required = false
    )
    private String userCidr;
}
