package com.stratocloud.provider.huawei.elb.listener.actions;

import com.stratocloud.form.BooleanField;
import com.stratocloud.form.InputField;
import com.stratocloud.form.NumberField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class HuaweiListenerBuildInput implements ResourceActionInput {
    @SelectField(
            label = "监听器协议",
            options = {"TCP", "UDP", "HTTP", "HTTPS", "QUIC", "TLS"},
            optionNames = {"TCP", "UDP", "HTTP", "HTTPS", "QUIC", "TLS"},
            defaultValues = "HTTP"
    )
    private String protocol;

    @NumberField(label = "监听端口", min = 1, max = 65535, defaultValue = 80)
    private Integer port;

    @InputField(
            label = "服务端SSL证书ID",
            conditions = "this.protocol === 'HTTPS' || this.protocol === 'TLS' || this.protocol === 'QUIC'"
    )
    private String defaultTlsContainerRef;

    @BooleanField(label = "开启高级转发策略", defaultValue = true)
    private Boolean enhancePolicyEnabled;
}
