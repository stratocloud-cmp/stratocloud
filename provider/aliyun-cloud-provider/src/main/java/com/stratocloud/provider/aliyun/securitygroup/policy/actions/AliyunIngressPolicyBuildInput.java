package com.stratocloud.provider.aliyun.securitygroup.policy.actions;

import com.stratocloud.constant.RegexExpressions;
import com.stratocloud.form.InputField;
import com.stratocloud.form.NumberField;
import com.stratocloud.form.SelectField;
import com.stratocloud.ip.InternetProtocol;
import com.stratocloud.provider.aliyun.common.AliyunDescriptions;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class AliyunIngressPolicyBuildInput implements ResourceActionInput {

    @SelectField(
            label = "IP版本",
            options = {"IPv4", "IPv6"},
            optionNames = {"IPv4", "IPv6"},
            defaultValues = "IPv4"
    )
    private InternetProtocol internetProtocol;

    @SelectField(
            label = "协议",
            options = {"TCP","UDP","ICMP","ICMPv6","GRE","ALL"},
            optionNames = {"TCP","UDP","ICMP","ICMPv6","GRE","全部"},
            defaultValues = "TCP"
    )
    private String protocol;

    @InputField(
            label = "端口",
            conditions = "this.protocol === 'TCP' || this.protocol === 'UDP'",
            description = AliyunDescriptions.POLICY_PORT_RANGE,
            defaultValue = "80/80"
    )
    private String portRange;


    @InputField(
            label = "目的地址(IPv4)",
            conditions = "this.internetProtocol === 'IPv4'",
            regex = RegexExpressions.IPV4_CIDR_REGEX,
            regexMessage = "CIDR格式不正确",
            required = false
    )
    private String destCidrIp;
    @InputField(
            label = "目的地址(IPv6)",
            conditions = "this.internetProtocol === 'IPv6'",
            regex = RegexExpressions.IPV6_CIDR_REGEX,
            regexMessage = "CIDR格式不正确",
            required = false
    )
    private String ipv6DestCidrIp;


    @InputField(
            label = "源端口",
            conditions = "this.protocol === 'TCP' || this.protocol === 'UDP'",
            description = AliyunDescriptions.POLICY_PORT_RANGE,
            required = false
    )
    private String sourcePortRange;


    @InputField(
            label = "源地址(IPv4)",
            conditions = "this.internetProtocol === 'IPv4'",
            regex = RegexExpressions.IPV4_CIDR_REGEX,
            regexMessage = "CIDR格式不正确",
            defaultValue = "0.0.0.0/0"
    )
    private String sourceCidrIp;
    @InputField(
            label = "源地址(IPv6)",
            conditions = "this.internetProtocol === 'IPv6'",
            regex = RegexExpressions.IPV6_CIDR_REGEX,
            regexMessage = "CIDR格式不正确"
    )
    private String ipv6SourceCidrIp;


    @SelectField(
            label = "策略",
            options = {"accept", "drop"},
            optionNames = {"允许", "拒绝"},
            defaultValues = "accept"
    )
    private String policy;

    @NumberField(label = "优先级",defaultValue = 1, max = 100)
    private Long priority;

}
