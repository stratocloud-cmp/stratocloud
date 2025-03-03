package com.stratocloud.provider.tencent.eip.actions;

import com.stratocloud.form.NumberField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class TencentBandwidthPackageBuildInput implements ResourceActionInput {
    @SelectField(
            label = "带宽包类型",
            options = {
                    "BGP",
                    "HIGH_QUALITY_BGP",
                    "SINGLEISP_CMCC",
                    "SINGLEISP_CTCC",
                    "SINGLEISP_CUCC"
            },
            optionNames = {
                    "普通BGP共享带宽包",
                    "精品BGP共享带宽包",
                    "中国移动共享带宽包",
                    "中国电信共享带宽包",
                    "中国联通共享带宽包"
            },
            defaultValues = "BGP"
    )
    private String networkType;
    @SelectField(
            label = "计费方式",
            options = {
                    TencentBandwidthChargeType.Ids.TOP5_POSTPAID_BY_MONTH,
                    TencentBandwidthChargeType.Ids.PERCENT95_POSTPAID_BY_MONTH,
                    TencentBandwidthChargeType.Ids.ENHANCED95_POSTPAID_BY_MONTH,
                    TencentBandwidthChargeType.Ids.FIXED_PREPAID_BY_MONTH,
                    TencentBandwidthChargeType.Ids.PEAK_BANDWIDTH_POSTPAID_BY_DAY,
            },
            optionNames = {
                    TencentBandwidthChargeType.Names.TOP5_POSTPAID_BY_MONTH,
                    TencentBandwidthChargeType.Names.PERCENT95_POSTPAID_BY_MONTH,
                    TencentBandwidthChargeType.Names.ENHANCED95_POSTPAID_BY_MONTH,
                    TencentBandwidthChargeType.Names.FIXED_PREPAID_BY_MONTH,
                    TencentBandwidthChargeType.Names.PEAK_BANDWIDTH_POSTPAID_BY_DAY,
            }
    )
    private String chargeType;
    @NumberField(
            label = "购买时长(月)",
            min = 1,
            max = 60,
            conditions = "this.chargeType ==='FIXED_PREPAID_BY_MONTH'"
    )
    private Long timeSpan;

    @NumberField(
            label = "带宽包限速大小(Mbps)",
            min = -1
    )
    private Long internetMaxBandwidth;
    @SelectField(
            label = "协议类型",
            options = {"ipv4", "ipv6"},
            optionNames = {"ipv4", "ipv6"},
            defaultValues = "ipv4"
    )
    private String protocol;


}
