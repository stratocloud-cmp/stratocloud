package com.stratocloud.provider.aliyun.nic.actions;

import com.stratocloud.form.IpField;
import com.stratocloud.form.NumberField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.aliyun.nic.AliyunNicTrafficMode;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

import java.util.List;

@Data
public class AliyunNicBuildInput implements ResourceActionInput {
    @SelectField(
            label = "Traffic模式",
            options = {"Standard", "HighPerformance"},
            optionNames = {"TCP 通讯模式(标准)", "RDMA 通讯模式(仅支持RDMA规格族)"},
            defaultValues = {"Standard"},
            description = "RDMA 通讯模式只支持 RDMA 增强型实例规格族 c7re，目前仅支持在华北 2（北京）的可用区 K 设置该参数值"
    )
    private AliyunNicTrafficMode trafficMode;
    @IpField(label = "指定内网IP", placeHolder = "请选择IP或直接输入, 留空将自动分配主IP")
    private List<String> ips;
    @NumberField(label = "额外辅助IP数量", required = false)
    private Integer secondaryPrivateIpAddressCount;
}
