package com.stratocloud.provider.huawei.elb.pool.actions;

import com.stratocloud.form.BooleanField;
import com.stratocloud.form.InputField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class HuaweiLbPoolBuildInput implements ResourceActionInput {
    @SelectField(
            label = "后端协议",
            options = {"TCP", "UDP", "HTTP"},
            optionNames = {"TCP", "UDP", "HTTP"},
            defaultValues = "HTTP"
    )
    private String protocol;

    @SelectField(
            label = "负载均衡算法",
            options = {"ROUND_ROBIN", "LEAST_CONNECTIONS", "SOURCE_IP"},
            optionNames = {"加权轮询算法", "加权最少连接算法", "源IP算法"},
            defaultValues = "ROUND_ROBIN"
    )
    private String lbAlgorithm;

    @BooleanField(
            label = "会话保持"
    )
    private boolean enableSessionPersistence;

    @SelectField(
            label = "会话保持类型",
            options = {"SOURCE_IP", "HTTP_COOKIE", "APP_COOKIE"},
            optionNames = {
                    "源IP",
                    "负载均衡器植入Cookie",
                    "后端服务器重写Cookie"
            },
            conditions = "this.enableSessionPersistence === true"
    )
    private String sessionPersistenceType;

    @InputField(
            label = "Cookie名称",
            conditions = "this.enableSessionPersistence === true && this.sessionPersistenceType === 'APP_COOKIE'")
    private String cookieName;
}
