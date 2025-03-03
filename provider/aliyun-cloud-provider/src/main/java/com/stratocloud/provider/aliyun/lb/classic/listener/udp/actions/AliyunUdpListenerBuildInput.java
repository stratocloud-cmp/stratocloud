package com.stratocloud.provider.aliyun.lb.classic.listener.udp.actions;

import com.stratocloud.form.BooleanField;
import com.stratocloud.form.InputField;
import com.stratocloud.form.NumberField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class AliyunUdpListenerBuildInput implements ResourceActionInput {
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


    @InputField(
            label = "UDP监听健康检查的请求字符串",
            conditions = "this.enableHealthCheck === true",
            description = "只允许包含字母、数字，最大长度限制为 64 个字符"
    )
    private String healthCheckReq;

    @InputField(
            label = "UDP监听健康检查的响应字符串",
            conditions = "this.enableHealthCheck === true",
            description = "只允许包含字母、数字，最大长度限制为 64 个字符"
    )
    private String healthCheckExp;

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
}
