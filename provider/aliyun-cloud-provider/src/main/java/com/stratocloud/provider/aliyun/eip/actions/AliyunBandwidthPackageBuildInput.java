package com.stratocloud.provider.aliyun.eip.actions;

import com.stratocloud.form.NumberField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.aliyun.eip.AliyunBwpChargeType;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class AliyunBandwidthPackageBuildInput implements ResourceActionInput {
    @SelectField(
            label = "带宽包类型",
            options = {
                    "BGP",
                    "BGP_PRO",
                    "ChinaTelecom",
                    "ChinaUnicom",
                    "ChinaMobile",
                    "ChinaTelecom_L2",
                    "ChinaUnicom_L2",
                    "ChinaMobile_L2",
                    "BGP_FinanceCloud"
            },
            optionNames = {
                    "普通BGP",
                    "精品BGP",
                    "中国电信",
                    "中国联通",
                    "中国移动",
                    "中国电信L2",
                    "中国联通L2",
                    "中国移动L2",
                    "金融云BGP"
            },
            defaultValues = "BGP"
    )
    private String isp;
    @SelectField(
            label = "计费方式",
            options = {
                    AliyunBwpChargeType.Ids.PAY_BY_BANDWIDTH,
                    AliyunBwpChargeType.Ids.PAY_BY_95,
                    AliyunBwpChargeType.Ids.PAY_BY_DOMINANT_TRAFFIC
            },
            optionNames = {
                    AliyunBwpChargeType.Names.PAY_BY_BANDWIDTH,
                    AliyunBwpChargeType.Names.PAY_BY_95,
                    AliyunBwpChargeType.Names.PAY_BY_DOMINANT_TRAFFIC,
            },
            defaultValues = AliyunBwpChargeType.Ids.PAY_BY_BANDWIDTH
    )
    private AliyunBwpChargeType chargeType;

    @NumberField(
            label = "共享带宽的带宽峰值(Mbps)",
            min = 1,
            max = 1000,
            defaultValue = 1
    )
    private Integer bandwidth;

    @NumberField(
            label = "共享带宽的保底百分比",
            min = 1,
            max = 100,
            defaultValue = 20,
            conditions = "this.chargeType === 'PayBy95'"
    )
    private Integer ratio;
}
