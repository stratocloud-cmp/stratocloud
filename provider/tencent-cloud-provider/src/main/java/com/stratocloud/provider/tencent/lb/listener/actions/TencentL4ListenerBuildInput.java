package com.stratocloud.provider.tencent.lb.listener.actions;

import com.stratocloud.form.BooleanField;
import com.stratocloud.form.InputField;
import com.stratocloud.form.NumberField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.tencent.common.TencentInputDescriptions;
import lombok.Data;

@Data
public class TencentL4ListenerBuildInput implements ResourceActionInput {
    @NumberField(label = "监听端口")
    private Long port;

    @SelectField(
            label = "监听器协议",
            options = {
                    "TCP", "UDP", "TCP_SSL", "QUIC"
            },
            optionNames = {
                    "TCP", "UDP", "TCP_SSL", "QUIC"
            }
    )
    private String protocol;


    @NumberField(
            label = "会话保持时间",
            min = 30,
            max = 3600,
            required = false,
            placeHolder = "留空代表不开启",
            conditions = "this.protocol === 'TCP' || this.protocol === 'UDP'"
    )
    private Long sessionExpireTime;

    @SelectField(
            label = "转发方式",
            options = {"WRR", "LEAST_CONN"},
            optionNames = {"按权重轮询", "最小连接数"},
            defaultValues = "WRR"
    )
    private String scheduler;


    @SelectField(
            label = "会话保持类型",
            options = {"NORMAL", "QUIC_CID"},
            optionNames = {"默认", "Quic Connection ID"},
            defaultValues = "NORMAL"
    )
    private String sessionType;


    @BooleanField(label = "解绑后端目标时，是否发RST给客户端", conditions = "this.protocol==='TCP'")
    private Boolean deregisterTargetRst;

    @NumberField(
            label = "监听器最大连接数",
            required = false
    )
    private Long maxConn;

    @NumberField(
            label = "监听器最大新增连接数",
            required = false
    )
    private Long maxCps;

    @NumberField(
            label = "空闲连接超时时间(秒)",
            required = false,
            conditions = "this.protocol==='TCP'"
    )
    private Long idleConnectTimeout;

    @BooleanField(label = "开启SNAT")
    private Boolean enableSnat;


    @BooleanField(label = "开启健康检查")
    private Boolean enableHealthCheck;
    @NumberField(label = "健康检查超时时间(秒)", conditions = "this.enableHealthCheck === true", defaultValue = 2)
    private Long healthCheckTimeout;
    @NumberField(label = "健康检查间隔时间(秒)", conditions = "this.enableHealthCheck === true", defaultValue = 5)
    private Long healthCheckInterval;
    @NumberField(
            label = "健康阈值",
            conditions = "this.enableHealthCheck === true",
            defaultValue = 3,
            description = "当连续探测多少次健康则表示该转发正常"
    )
    private Long healthyNumber;
    @NumberField(
            label = "不健康阈值",
            conditions = "this.enableHealthCheck === true",
            defaultValue = 3,
            description = "当连续探测多少次不健康则表示该转发异常"
    )
    private Long unhealthyNumber;
    @NumberField(
            label = "健康检查状态码",
            conditions = {
                    "this.enableHealthCheck === true",
                    "this.protocol==='TCP'"
            },
            defaultValue = 31,
            description = TencentInputDescriptions.HealthCheckHttpCode,
            required = false
    )
    private Long httpCode;
    @InputField(
            label = "健康检查路径",
            conditions = {
                    "this.enableHealthCheck === true",
                    "this.protocol==='TCP'"
            },
            required = false
    )
    private String httpCheckPath;
    @InputField(
            label = "健康检查域名",
            conditions = {
                    "this.enableHealthCheck === true",
                    "this.protocol==='TCP'"
            },
            description = TencentInputDescriptions.HealthCheckDomain,
            required = false
    )
    private String httpCheckDomain;

    @SelectField(
            label = "健康检查方法",
            conditions = {
                    "this.enableHealthCheck === true",
                    "this.protocol==='TCP'"
            },
            options = {"HEAD", "GET"},
            optionNames = {"HEAD", "GET"},
            defaultValues = "HEAD"
    )
    private String httpCheckMethod;
    @NumberField(
            label = "健康检查自定义探测端口",
            description = "默认为后端服务的端口，除非您希望指定特定端口，否则建议留空",
            required = false,
            conditions = {
                    "this.enableHealthCheck === true",
                    "this.protocol==='TCP' || this.protocol==='UDP'"
            }
    )
    private Long checkPort;

    @SelectField(
            label = "健康检查协议类型",
            options = {"TCP","HTTP","HTTPS","GRPC","PING"},
            optionNames = {"TCP","HTTP","HTTPS","GRPC","PING"},
            conditions = {
                    "this.enableHealthCheck === true",
            },
            description = TencentInputDescriptions.HealthCheckType,
            required = false
    )
    private String checkType;

    @SelectField(
            label = "健康检查HTTP版本",
            conditions = {
                    "this.enableHealthCheck === true",
                    "this.checkType === 'HTTP'",
                    "this.protocol==='TCP'"
            },
            options = {"HTTP/1.0","HTTP/1.1"},
            optionNames = {"HTTP/1.0","HTTP/1.1"}
    )
    private String httpVersion;

    @SelectField(
            label = "健康检查源IP类型",
            options = {"0", "1"},
            optionNames = {"使用LB的VIP作为源IP", "使用100.64网段IP作为源IP"},
            conditions = "this.enableHealthCheck === true",
            required = false
    )
    private Long sourceIpType;
}
