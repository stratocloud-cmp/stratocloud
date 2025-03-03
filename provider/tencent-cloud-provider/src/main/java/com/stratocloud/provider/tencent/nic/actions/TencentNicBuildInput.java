package com.stratocloud.provider.tencent.nic.actions;

import com.stratocloud.form.IpField;
import com.stratocloud.form.NumberField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.tencent.nic.NicQosLevel;
import com.stratocloud.provider.tencent.nic.NicTrunkingFlag;
import lombok.Data;

import java.util.List;

@Data
public class TencentNicBuildInput implements ResourceActionInput {
    @SelectField(
            label = "服务质量级别",
            options = {"PT", "AU", "AG", "DEFAULT"},
            optionNames = {"云金", "云银", "云铜", "默认级别"},
            defaultValues = {"DEFAULT"}
    )
    private NicQosLevel qosLevel;
    @SelectField(
            label = "Trunking模式",
            options = {"Enable", "Disable"},
            optionNames = {"开启", "关闭"},
            defaultValues = {"Disable"}
    )
    private NicTrunkingFlag trunkingFlag;
    @IpField(label = "指定内网IP", placeHolder = "请选择IP或直接输入, 留空将自动分配主IP")
    private List<String> ips;
    @NumberField(label = "额外辅助IP数量", required = false)
    private Integer secondaryPrivateIpAddressCount;
}
