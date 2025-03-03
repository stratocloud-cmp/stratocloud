package com.stratocloud.provider.aliyun.lb.classic.listener.http.actions;

import com.stratocloud.form.BooleanField;
import com.stratocloud.form.InputField;
import com.stratocloud.form.NumberField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.aliyun.common.AliyunDescriptions;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

import java.util.List;

@Data
public class AliyunHttpListenerBuildInput implements ResourceActionInput {
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
            label = "是否开启通过X-Forwarded-For头字段获取来访客户端IP",
            options = {"on", "off"},
            optionNames = {"是", "否"},
            defaultValues = "on"
    )
    private String xForwardedFor;

    @SelectField(
            label = "调度算法",
            options = {"wrr", "rr"},
            optionNames = {"按权重轮询", "按顺序轮询"},
            defaultValues = "wrr"
    )
    private String scheduler;

    @SelectField(
            label = "是否开启会话保持",
            options = {"on", "off"},
            optionNames = {"是", "否"},
            defaultValues = "off"
    )
    private String stickySession;

    @SelectField(
            label = "Cookie 处理方式",
            options = {"insert", "server"},
            optionNames = {"植入 Cookie", "重写 Cookie"},
            conditions = "this.stickySession === 'on'"
    )
    private String stickySessionType;

    @NumberField(
            label = "Cookie超时时间(秒)",
            min = 1,
            max = 86400,
            defaultValue = 500,
            conditions = "this.stickySession === 'on' && this.stickySessionType==='insert'"
    )
    private Integer cookieTimeout;

    @InputField(
            label = "服务器上配置的 Cookie",
            inputType = "textarea",
            conditions = "this.stickySession === 'on' && this.stickySessionType==='server'"
    )
    private String cookie;


    @BooleanField(
            label = "开启健康检查"
    )
    private Boolean enableHealthCheck;

    @SelectField(
            label = "健康检查方法",
            options = {"head", "get"},
            optionNames = {"head", "get"},
            conditions = "this.enableHealthCheck === true",
            defaultValues = "get"
    )
    private String healthCheckMethod;

    @InputField(
            label = "健康检查域名",
            conditions = "this.enableHealthCheck === true",
            description = AliyunDescriptions.HEALTH_CHECK_DOMAIN,
            required = false
    )
    private String healthCheckDomain;

    @InputField(
            label = "健康检查URI",
            conditions = "this.enableHealthCheck === true",
            description = AliyunDescriptions.HEALTH_CHECK_URI
    )
    private String healthCheckUri;

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

    @SelectField(
            label = "健康检查正常状态码",
            options = {"http_2xx", "http_3xx", "http_4xx", "http_5xx"},
            optionNames = {"http_2xx", "http_3xx", "http_4xx", "http_5xx"},
            multiSelect = true,
            allowCreate = true,
            defaultValues = "http_2xx",
            conditions = "this.enableHealthCheck === true"
    )
    private List<String> healthCheckHttpCode;

    @SelectField(
            label = "是否通过SLB-IP头字段获取客户端请求的VIP",
            options = {"on", "off"},
            optionNames = {"是", "否"},
            defaultValues = "off"
    )
    private String xForwardedFor_SLBIP;

    @SelectField(
            label = "是否通过SLB-ID头字段获取负载均衡实例ID",
            options = {"on", "off"},
            optionNames = {"是", "否"},
            defaultValues = "off"
    )
    private String xForwardedFor_SLBID;

    @SelectField(
            label = "是否通过X-Forwarded-Proto头字段获取负载均衡实例的监听协议",
            options = {"on", "off"},
            optionNames = {"是", "否"},
            defaultValues = "off"
    )
    private String xForwardedFor_proto;

    @SelectField(
            label = "是否开启Gzip压缩，对特定文件类型进行压缩",
            options = {"on", "off"},
            optionNames = {"是", "否"},
            defaultValues = "on"
    )
    private String gzip;

    @SelectField(
            label = "是否开启HTTP至HTTPS的转发",
            options = {"on", "off"},
            optionNames = {"是", "否"},
            defaultValues = "off"
    )
    private String listenerForward;

    @NumberField(
            label = "HTTP至HTTPS的监听转发端口",
            defaultValue = 443,
            min = 1,
            max = 65535,
            conditions = "this.listenerForward === 'on'"
    )
    private Integer forwardPort;

    @NumberField(
            label = "连接空闲超时时间(秒)",
            min = 1,
            max = 60,
            defaultValue = 15
    )
    private Integer idleTimeout;

    @NumberField(
            label = "请求超时时间(秒)",
            min = 1,
            max = 180,
            defaultValue = 60,
            description = "在超时时间内后端服务器一直没有响应，负载均衡将放弃等待，给客户端返回HTTP 504错误码"
    )
    private Integer requestTimeout;


    @SelectField(
            label = "是否通过XForwardedFor_SLBPORT头字段获取负载均衡实例的监听端口",
            options = {"on", "off"},
            optionNames = {"是", "否"},
            defaultValues = "off"
    )
    private String xForwardedFor_SLBPORT;

    @SelectField(
            label = "是否通过XForwardedFor_ClientSrcPort头字段获取访问负载均衡实例客户端的端口",
            options = {"on", "off"},
            optionNames = {"是", "否"},
            defaultValues = "off"
    )
    private String xForwardedFor_ClientSrcPort;
}
