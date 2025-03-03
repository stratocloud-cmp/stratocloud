package com.stratocloud.provider.aliyun.lb.classic.listener.tcp.actions;

import com.stratocloud.form.BooleanField;
import com.stratocloud.form.InputField;
import com.stratocloud.form.NumberField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.aliyun.common.AliyunDescriptions;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

import java.util.List;

@Data
public class AliyunTcpListenerBuildInput implements ResourceActionInput {
    @NumberField(
            label = "带宽峰值(Mbps)",
            description = "-1表示不限制",
            defaultValue = -1,
            min = -1
    )
    private Integer bandwidth;

    @NumberField(
            label = "监听端口",
            min = 1,
            max = 65535,
            defaultValue = 80
    )
    private Integer port;

    @NumberField(
            label = "后端服务器端口",
            min = 1,
            max = 65535,
            defaultValue = 80
    )
    private Integer backendPort;

    @SelectField(
            label = "调度算法",
            options = {"wrr", "rr"},
            optionNames = {"按权重轮询", "按顺序轮询"},
            defaultValues = "wrr"
    )
    private String scheduler;


    @BooleanField(
            label = "开启健康检查",
            defaultValue = true
    )
    private Boolean enableHealthCheck;



    @SelectField(
            label = "健康检查方法",
            options = {"tcp", "http"},
            optionNames = {"TCP", "HTTP"},
            conditions = "this.enableHealthCheck === true",
            defaultValues = "tcp"
    )
    private String healthCheckType;

    @InputField(
            label = "健康检查域名",
            conditions = "this.enableHealthCheck === true",
            description = AliyunDescriptions.HEALTH_CHECK_DOMAIN,
            required = false
    )
    private String healthCheckDomain;

    @InputField(
            label = "健康检查URI",
            conditions = "this.enableHealthCheck === true && this.healthCheckType === 'http'",
            description = AliyunDescriptions.HEALTH_CHECK_URI
    )
    private String healthCheckUri;

    @SelectField(
            label = "健康检查正常状态码",
            options = {"http_2xx", "http_3xx", "http_4xx", "http_5xx"},
            optionNames = {"http_2xx", "http_3xx", "http_4xx", "http_5xx"},
            multiSelect = true,
            allowCreate = true,
            defaultValues = "http_2xx",
            conditions = "this.enableHealthCheck === true && this.healthCheckType === 'http'"
    )
    private List<String> healthCheckHttpCode;

    @NumberField(
            label = "健康检查连续成功多少次后，将健康检查状态判定为成功",
            defaultValue = 4,
            min = 2,
            max = 10,
            conditions = "this.enableHealthCheck === true"
    )
    private Integer healthyThreshold;

    @NumberField(
            label = "健康检查连续失败多少次后，将健康检查状态判定为失败",
            defaultValue = 4,
            min = 2,
            max = 10,
            conditions = "this.enableHealthCheck === true"
    )
    private Integer unhealthyThreshold;

    @NumberField(
            label = "健康检查超时时间(秒)",
            min = 1,
            max = 300,
            defaultValue = 5,
            conditions = "this.enableHealthCheck === true"
    )
    private Integer healthCheckTimeout;

    @NumberField(
            label = "健康检查端口",
            defaultValue = 80,
            min = 1,
            max = 65535,
            conditions = "this.enableHealthCheck === true"
    )
    private Integer healthCheckConnectPort;

    @NumberField(
            label = "健康检查间隔(秒)",
            defaultValue = 5,
            min = 1,
            max = 50,
            conditions = "this.enableHealthCheck === true"
    )
    private Integer healthCheckInterval;


    @NumberField(
            label = "会话保持超时时间(秒)",
            min = 1,
            max = 60,
            defaultValue = 15
    )
    private Integer persistenceTimeout;

    @NumberField(
            label = "连接超时时间(秒)",
            min = 1,
            max = 180,
            defaultValue = 60,
            description = "在超时时间内后端服务器一直没有响应，负载均衡将放弃等待，给客户端返回HTTP 504错误码"
    )
    private Integer establishedTimeout;


    @SelectField(
            label = "是否开启连接优雅中断",
            options = {"on", "off"},
            optionNames = {"开启", "不开启"},
            defaultValues = "off"
    )
    private String connectionDrain;

    @NumberField(
            label = "设置连接优雅中断超时时间(秒)",
            min = 10,
            max = 900,
            defaultValue = 300,
            conditions = "this.connectionDrain === 'on'"
    )
    private Integer connectionDrainTimeout;
}
