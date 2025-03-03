package com.stratocloud.provider.tencent.lb.rule.actions;

import com.stratocloud.form.BooleanField;
import com.stratocloud.form.InputField;
import com.stratocloud.form.NumberField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.tencent.common.TencentInputDescriptions;
import lombok.Data;

import java.util.List;

@Data
public class TencentL7RuleBuildInput implements ResourceActionInput {
    @InputField(label = "转发规则路径")
    private String url;
    @SelectField(label = "转发规则域名", multiSelect = true, allowCreate = true)
    private List<String> domains;
    @NumberField(label = "会话保持时间(秒)", required = false)
    private Long sessionExpireTime;

    @SelectField(
            label = "转发方式",
            options = {"WRR", "LEAST_CONN"},
            optionNames = {"按权重轮询", "最小连接数"},
            defaultValues = "WRR"
    )
    private String scheduler;
    @SelectField(
            label = "转发协议",
            options = {
                    "HTTP", "HTTPS", "GRPC"
            },
            optionNames = {
                    "HTTP", "HTTPS", "GRPC"
            },
            defaultValues = "HTTP"
    )
    private String forwardType;

    @BooleanField(label = "是否将该域名设为默认域名")
    private Boolean defaultServer;

    @BooleanField(label = "启用HTTP2", description = "HTTPS域名才可启用此项")
    private Boolean enableHttp2;

    @BooleanField(label = "启用QUIC", description = "HTTPS域名才可启用此项")
    private Boolean enableQuic;


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
            conditions = "this.enableHealthCheck === true",
            defaultValue = 31,
            description = TencentInputDescriptions.HealthCheckHttpCode
    )
    private Long httpCode;
    @InputField(
            label = "健康检查路径",
            conditions = "this.enableHealthCheck === true",
            required = false
    )
    private String httpCheckPath;
    @InputField(
            label = "健康检查域名",
            conditions = "this.enableHealthCheck === true",
            description = TencentInputDescriptions.HealthCheckDomain,
            required = false
    )
    private String httpCheckDomain;

    @SelectField(
            label = "健康检查方法",
            conditions = "this.enableHealthCheck === true",
            options = {"HEAD", "GET"},
            optionNames = {"HEAD", "GET"},
            defaultValues = "HEAD",
            required = false
    )
    private String httpCheckMethod;

    @SelectField(
            label = "健康检查源IP类型",
            options = {"0", "1"},
            optionNames = {"使用LB的VIP作为源IP", "使用100.64网段IP作为源IP"},
            conditions = "this.enableHealthCheck === true",
            required = false
    )
    private Long sourceIpType;
}
