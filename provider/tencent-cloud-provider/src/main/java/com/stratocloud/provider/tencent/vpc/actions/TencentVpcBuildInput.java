package com.stratocloud.provider.tencent.vpc.actions;

import com.stratocloud.constant.RegexExpressions;
import com.stratocloud.form.BooleanField;
import com.stratocloud.form.InputField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

import java.util.List;

@Data
public class TencentVpcBuildInput implements ResourceActionInput {
    @InputField(
            label = "CIDR",
            regex = RegexExpressions.IPV4_CIDR_REGEX,
            regexMessage = "CIDR格式不正确",
            description = "仅能在10.0.0.0/12，172.16.0.0/12，192.168.0.0/16这三个内网网段内。示例值：10.8.0.0/16"
    )
    private String cidrBlock;

    @BooleanField(label = "开启多播")
    private Boolean enableMultiCast = false;

    @SelectField(
            label = "DNS地址",
            multiSelect = true,
            allowCreate = true,
            description = "DNS地址，最多支持4个。如无特殊要求留空即可。",
            required = false
    )
    private List<String> dnsServers;
    @InputField(
            label = "DHCP域名",
            description = "DHCP域名。如无特殊要求留空即可。",
            required = false
    )
    private String domainName;
}
