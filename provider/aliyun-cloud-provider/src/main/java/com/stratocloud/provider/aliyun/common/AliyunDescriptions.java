package com.stratocloud.provider.aliyun.common;

public class AliyunDescriptions {
    public static final String VPC_CIDR = """
            建议您使用 192.168.0.0/16、172.16.0.0/12、10.0.0.0/8 三个 RFC 标准私网网段
            及其子网作为专有网络的主 IPv4 网段，网段掩码有效范围为 8~28 位。
            您也可以使用除 100.64.0.0/10、224.0.0.0/4、127.0.0.0/8 或 169.254.0.0/16
            及其子网外的自定义地址段作为专有网络的主 IPv4 网段。
            """;

    public static final String SUBNET_CIDR = """
            交换机的网段。交换机网段要求如下：
                        
            交换机的网段的掩码长度范围为 16～29 位。
            交换机的网段必须从属于所在 VPC 的网段。
            交换机的网段不能与所在 VPC 中路由条目的目标网段相同，但可以是目标网段的子集。
            交换机的网段不能是 100.64.0.0/10 及其子网网段。
            
            示例值:
            172.16.0.0/24
            """;
    public static final String POLICY_PORT_RANGE = """
            安全组开放的各协议相关的目的端口范围。取值范围：
            取值范围为 1~65535。使用正斜线（/）隔开起始端口和终止端口。例如：1/200。
            """;
    public static final String INSTANCE_PASSWORD= """
            实例的密码。长度为 8 至 30 个字符，必须同时包含大小写英文字母、数字和特殊符号中的三类字符。特殊符号可以是：
            ()`~!@#$%^&*-_+=|{}[]:;'<>,.?/
            其中，Windows 实例不能以正斜线（/）为密码首字符。
            """;
    public static final String INSTANCE_DELETION_PROTECTION = """
            实例释放保护属性，指定是否支持通过控制台或 API（ DeleteInstance ）释放实例。
            """;

    public static final String HEALTH_CHECK_DOMAIN = """
            用于健康检查的域名，取值：
            $_ip： 后端服务器的私网 IP。当指定了 IP 或该参数未指定时，负载均衡会使用各后端服务器的私网 IP 当做健康检查使用的域名。
            domain：域名长度为 1~80 字符，只能包含字母、数字、半角句号（.）和短划线（-）。
            """;
    public static final String HEALTH_CHECK_URI = """
            用于健康检查的 URI。
            长度限制为 1~80 个字符，只能使用字母、数字和短划线（-）、正斜线（/）、半角句号（.）、百分号（%）、半角问号（?）、井号（#）和 and（&）这些字符。
            URI 不能只为正斜线（/），但必须以正斜线（/）开头。
            """;

}
