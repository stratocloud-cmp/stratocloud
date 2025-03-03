package com.stratocloud.provider.tencent.securitygroup.policy.actions;

import com.stratocloud.constant.RegexExpressions;
import com.stratocloud.form.InputField;
import com.stratocloud.form.NumberField;
import com.stratocloud.form.SelectField;
import com.stratocloud.ip.InternetProtocol;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.tencent.common.TencentInputDescriptions;
import com.tencentcloudapi.vpc.v20170312.models.SecurityGroupPolicy;
import lombok.Data;

@Data
public class TencentIngressPolicyBuildInput implements ResourceActionInput {

    @SelectField(
            label = "IP版本",
            options = {"IPv4", "IPv6"},
            optionNames = {"IPv4", "IPv6"}
    )
    private InternetProtocol internetProtocol;

    @SelectField(
            label = "协议",
            options = {"TCP","UDP","ICMP","ICMPv6","GRE","ALL"},
            optionNames = {"TCP","UDP","ICMP","ICMPv6","GRE","全部"}
    )
    private String protocol;

    @InputField(
            label = "端口",
            conditions = "this.protocol === 'TCP' || this.protocol === 'UDP'",
            description = TencentInputDescriptions.SecurityGroupPolicyPort
    )
    private String port;


    @InputField(
            label = "源地址(IPv4)",
            conditions = "this.internetProtocol === 'IPv4'",
            regex = RegexExpressions.IPV4_CIDR_REGEX,
            regexMessage = "CIDR格式不正确",
            defaultValue = "0.0.0.0/0"
    )
    private String cidrBlock;
    @InputField(
            label = "源地址(IPv6)",
            conditions = "this.internetProtocol === 'IPv6'",
            regex = RegexExpressions.IPV6_CIDR_REGEX,
            regexMessage = "CIDR格式不正确",
            defaultValue = "::/0"
    )
    private String ipv6CidrBlock;

    @SelectField(
            label = "策略",
            options = {"ACCEPT", "DROP"},
            optionNames = {"允许", "拒绝"}
    )
    private String action;

    @NumberField(label = "插入位置",placeHolder = "留空代表插入到最后",required = false)
    private Long portIndex;


    public SecurityGroupPolicy createPolicy(){
        SecurityGroupPolicy policy = new SecurityGroupPolicy();

        policy.setPolicyIndex(portIndex);
        policy.setProtocol(protocol);
        policy.setPort(port);
        policy.setAction(action);

        switch (internetProtocol){
            case IPv4 -> policy.setCidrBlock(cidrBlock);
            case IPv6 -> policy.setIpv6CidrBlock(ipv6CidrBlock);
        }

        return policy;
    }
}
