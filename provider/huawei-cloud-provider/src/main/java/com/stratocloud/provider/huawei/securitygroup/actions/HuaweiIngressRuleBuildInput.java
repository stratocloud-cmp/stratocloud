package com.stratocloud.provider.huawei.securitygroup.actions;

import com.stratocloud.form.InputField;
import com.stratocloud.form.NumberField;
import com.stratocloud.form.SelectField;
import com.stratocloud.ip.InternetProtocol;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class HuaweiIngressRuleBuildInput implements ResourceActionInput {
    @SelectField(
            label = "IP版本",
            options = {"IPv4", "IPv6"},
            optionNames = {"IPv4", "IPv6"},
            defaultValues = "IPv4"
    )
    private InternetProtocol etherType;

    @InputField(
            label = "源地址",
            description = "当选择了源端安全组时该参数不会生效",
            defaultValue = "0.0.0.0/0"
    )
    private String remoteIpPrefix;

    @SelectField(
            label = "协议",
            options = {"tcp", "udp", "icmp", "any"},
            optionNames = {"TCP", "UDP", "ICMP", "ANY"},
            defaultValues = "any"
    )
    private String protocol;

    @NumberField(label = "起始端口", max = 65535, defaultValue = 1, conditions = "this.protocol !== 'any'")
    private Integer portMin;

    @NumberField(label = "终止端口", max = 65535, defaultValue = 65535, conditions = "this.protocol !== 'any'")
    private Integer portMax;


}
