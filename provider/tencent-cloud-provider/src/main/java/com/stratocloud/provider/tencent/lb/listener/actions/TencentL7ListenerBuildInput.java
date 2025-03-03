package com.stratocloud.provider.tencent.lb.listener.actions;

import com.stratocloud.form.BooleanField;
import com.stratocloud.form.NumberField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class TencentL7ListenerBuildInput implements ResourceActionInput {
    @NumberField(label = "监听端口")
    private Long port;

    @SelectField(
            label = "监听器协议",
            options = {
                    "HTTP", "HTTPS"
            },
            optionNames = {
                    "HTTP", "HTTPS"
            }
    )
    private String protocol;

    @BooleanField(label = "开启SNI特性", conditions = "this.protocol === 'HTTPS'")
    private Boolean enableSni;

    @BooleanField(label = "开启长连接")
    private Boolean keepAliveEnabled;

    @BooleanField(label = "开启SNAT")
    private Boolean enableSnat;
}
