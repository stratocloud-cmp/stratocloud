package com.stratocloud.provider.aliyun.lb.classic.actions;

import com.stratocloud.form.IpField;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AliyunIntranetClbBuildInput extends AliyunClbBuildInput {
    @IpField(label = "内网IP地址")
    private List<String> ipAddress;
}
