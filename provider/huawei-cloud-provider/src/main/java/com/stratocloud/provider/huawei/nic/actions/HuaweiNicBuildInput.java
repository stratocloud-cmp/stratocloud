package com.stratocloud.provider.huawei.nic.actions;

import com.stratocloud.form.BooleanField;
import com.stratocloud.form.IpField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

import java.util.List;

@Data
public class HuaweiNicBuildInput implements ResourceActionInput {
    @IpField(label = "指定内网IP", placeHolder = "请选择IP或直接输入, 留空将自动分配主IP")
    private List<String> ips;
    @BooleanField(label = "管理状态", defaultValue = true)
    private Boolean adminStateUp;
}
