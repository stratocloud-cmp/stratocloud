package com.stratocloud.provider.tencent.common;

public class TencentInputDescriptions {
    public static final String SecurityGroupPolicyPort =
            """
            允许输入一个单独端口号，例如80。
            或者用逗号分隔的若干个端口号，例如80,85,87。
            或者用减号分隔的两个端口号代表端口范围，例如8000-8010。
            或者用all代表全部端口。
            """;
    public static final String DisableApiTermination = """
            实例销毁保护标志，表示是否允许通过api接口删除实例
            """;


    public static final String InstancePassword = """
            实例登录密码。不同操作系统类型密码复杂度限制不一样，具体如下：
            Linux实例密码必须8到30位，至少包括两项[a-z]，[A-Z]、[0-9] 和 [( ) ` ~ ! @ # $ % ^ & * - + = | { } [ ] : ; ' , . ? / ]中的特殊符号。
            Windows实例密码必须12到30位，至少包括三项[a-z]，[A-Z]，[0-9] 和 [( ) ` ~ ! @ # $ % ^ & * - + = | { } [ ] : ; ' , . ? /]中的特殊符号。
                        
            若不指定该参数，则由系统随机生成密码，并通过站内信方式通知到用户。
            """;
    public static final String LbSlaType = """
            留空代表使用共享型实例，详细规格如下。
            共享型
            每分钟并发连接数：50,000，每秒新建连接数：5,000，每秒查询数：5,000
            标准型
            每分钟并发连接数：100,000，每秒新建连接数：10,000，每秒查询数：10,000，带宽上限：2Gbps
            高阶型1
            每分钟并发连接数：200,000，每秒新建连接数：20,000，每秒查询数：20,000，带宽上限：4Gbps
            高阶型2
            每分钟并发连接数：500,000，每秒新建连接数：50,000，每秒查询数：30,000，带宽上限：6Gbps
            超强型1
            每分钟并发连接数：1,000,000，每秒新建连接数：100,000，每秒查询数：50,000，带宽上限：10Gbps
            超强型2
            每分钟并发连接数：2,000,000，每秒新建连接数：200,000，每秒查询数：100,000，带宽上限：20Gbps
            超强型3
            每分钟并发连接数：5,000,000，每秒新建连接数：500,000，每秒查询数：200,000，带宽上限：40Gbps
            超强型4
            每分钟并发连接数：10,000,000，每秒新建连接数：1,000,000，每秒查询数：300,000，带宽上限：60Gbps
            """;

    public static final String HealthCheckHttpCode = """
            可选值：1~31，默认 31。 1 表示探测后返回值 1xx 代表健康，2 表示返回 2xx 代表健康，
            4 表示返回 3xx 代表健康，8 表示返回 4xx 代表健康，16 表示返回 5xx 代表健康。
            若希望多种返回码都可代表健康，则将相应的值相加。
            """;

    public static final String HealthCheckDomain = """
            健康检查域名（仅适用于HTTP/HTTPS监听器和TCP监听器的HTTP健康检查方式。
            针对TCP监听器，当使用HTTP健康检查方式时，该参数为必填项）
            """;

    public static final String HealthCheckType = """
            取值 TCP | HTTP | HTTPS | GRPC | PING | CUSTOM，UDP监听器支持PING/CUSTOM，TCP监听器支持TCP/HTTP/CUSTOM，
            TCP_SSL/QUIC监听器支持TCP/HTTP，HTTP规则支持HTTP/GRPC，HTTPS规则支持HTTP/HTTPS/GRPC。
            HTTP监听器默认值为HTTP;TCP、TCP_SSL、QUIC监听器默认值为TCP;UDP监听器默认为PING;
            HTTPS监听器的CheckType默认值与后端转发协议一致
            """;
}
