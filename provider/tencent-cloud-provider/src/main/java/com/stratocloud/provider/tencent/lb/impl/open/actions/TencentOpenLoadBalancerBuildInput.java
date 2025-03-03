package com.stratocloud.provider.tencent.lb.impl.open.actions;

import com.stratocloud.form.BooleanField;
import com.stratocloud.form.NumberField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.tencent.common.TencentInputDescriptions;
import lombok.Data;

@Data
public class TencentOpenLoadBalancerBuildInput implements ResourceActionInput {
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
            label = "IP版本",
            options = {"IPv4","IPv6","IPv6FullChain"},
            optionNames = {"IPv4","IPv6 NAT64","IPv6"}
    )
    private String ipVersion;

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
            defaultValues = "TRAFFIC_POSTPAID_BY_HOUR"
    )
    private String internetChargeType;

    @NumberField(
            label = "最大出带宽(Mbps)",
            min = 1,
            defaultValue = 10
    )
    private Integer internetMaxBandwidthOut;

    @SelectField(
            label = "运营商类型",
            options = {"BGP", "CMCC", "CUCC", "CTCC"},
            optionNames = {"默认", "中国移动", "中国联通", "中国电信"},
            defaultValues = "BGP",
            conditions = "this.internetChargeType === 'BANDWIDTH_PACKAGE'"
    )
    private String isp;



    @BooleanField(
            label = "创建域名化负载均衡"
    )
    private Boolean dynamicVip;
}
