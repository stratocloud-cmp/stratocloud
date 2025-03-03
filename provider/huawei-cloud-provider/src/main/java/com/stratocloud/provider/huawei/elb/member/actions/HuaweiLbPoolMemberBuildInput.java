package com.stratocloud.provider.huawei.elb.member.actions;

import com.stratocloud.constant.RegexExpressions;
import com.stratocloud.form.InputField;
import com.stratocloud.form.NumberField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class HuaweiLbPoolMemberBuildInput implements ResourceActionInput {
    @InputField(label = "后端IP地址", regex = RegexExpressions.IP_REGEX)
    private String address;
    @NumberField(label = "后端端口", min = 1, max = 65535, defaultValue = 80)
    private Integer backendPort;
    @NumberField(label = "权重", max = 100, defaultValue = 100)
    private Integer weight;
}
