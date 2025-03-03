package com.stratocloud.provider.tencent.lb.impl.internal.actions;

import com.stratocloud.form.BooleanField;
import com.stratocloud.form.IpField;
import com.stratocloud.form.NumberField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.tencent.common.TencentInputDescriptions;
import lombok.Data;

import java.util.List;

@Data
public class TencentInternalLoadBalancerBuildInput implements ResourceActionInput {
    @SelectField(
            label = "性能容量型规格",
            options = {
                    "clb.c2.medium",
                    "clb.c3.small",
                    "clb.c3.medium",
                    "clb.c4.small",
                    "clb.c4.large",
                    "clb.c4.xlarge"
            },
            optionNames = {
                    "标准型(clb.c2.medium)",
                    "高阶型1(clb.c3.small)",
                    "高阶型2(clb.c3.medium)",
                    "超强型1(clb.c4.small)",
                    "超强型2(clb.c4.medium)",
                    "超强型3(clb.c4.large)",
                    "超强型4(clb.c4.xlarge)"
            },
            description = TencentInputDescriptions.LbSlaType,
            required = false
    )
    private String slaType;

    @SelectField(
            label = "网络计费模式",
            options = {
                    "TRAFFIC_POSTPAID_BY_HOUR",
                    "BANDWIDTH_POSTPAID_BY_HOUR",
                    "BANDWIDTH_PACKAGE",
            },
            optionNames = {
                    "按流量按小时后付费",
                    "按带宽按小时后付费",
                    "按带宽包计费",
            },
            defaultValues = "TRAFFIC_POSTPAID_BY_HOUR",
            conditions = "this.slaType"
    )
    private String internetChargeType;

    @NumberField(
            label = "最大出带宽(Mbps)",
            min = 1,
            defaultValue = 10,
            conditions = "this.slaType"
    )
    private Integer internetMaxBandwidthOut;

    @IpField(label = "VIP地址")
    private List<String> vips;

    @BooleanField(
            label = "创建域名化负载均衡"
    )
    private Boolean dynamicVip;

}
