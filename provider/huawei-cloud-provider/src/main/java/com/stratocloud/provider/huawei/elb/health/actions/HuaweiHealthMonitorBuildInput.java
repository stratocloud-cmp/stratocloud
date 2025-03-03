package com.stratocloud.provider.huawei.elb.health.actions;

import com.stratocloud.form.InputField;
import com.stratocloud.form.NumberField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class HuaweiHealthMonitorBuildInput implements ResourceActionInput {
    @NumberField(label = "健康检查间隔(秒)", min = 1, max = 50, defaultValue = 10)
    private Integer delay;

    @NumberField(label = "置为健康所需次数", min = 1, max = 10, defaultValue = 3)
    private Integer maxRetries;

    @NumberField(label = "置为不健康所需次数", min = 1, max = 10, defaultValue = 3)
    private Integer maxRetriesDown;

    @NumberField(label = "健康检查超时时间(秒)", min = 1, max = 50, defaultValue = 5)
    private Integer timeout;

    @SelectField(
            label = "健康检查类型",
            options = {"TCP", "UDP_CONNECT", "HTTP"},
            optionNames = {"TCP", "UDP_CONNECT", "HTTP"},
            defaultValues = "HTTP"
    )
    private String type;

    @InputField(label = "健康检查路径", defaultValue = "/", conditions = "this.type === 'HTTP'")
    private String urlPath;
}
