package com.stratocloud.provider.tencent.eip.actions;

import com.stratocloud.form.NumberField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class TencentEipBuildInput implements ResourceActionInput {

    @SelectField(
            label = "EIP线路类型",
            options = {
                    "BGP",
                    "CMCC",
                    "CTCC",
                    "CUCC"
            },
            optionNames = {
                    "BGP(默认)",
                    "中国移动",
                    "中国电信",
                    "中国联通"
            },
            defaultValues = "BGP"
    )
    private String internetServiceProvider;

    @SelectField(
            label = "计费方式",
            options = {
                    "BANDWIDTH_PACKAGE",
                    "BANDWIDTH_POSTPAID_BY_HOUR",
                    "BANDWIDTH_PREPAID_BY_MONTH",
                    "TRAFFIC_POSTPAID_BY_HOUR"
            },
            optionNames = {
                    "共享带宽包",
                    "带宽按小时后付费",
                    "包月按带宽预付费",
                    "流量按小时后付费"
            },
            defaultValues = "TRAFFIC_POSTPAID_BY_HOUR"
    )
    private String internetChargeType;
    @SelectField(
            label = "购买时长",
            options = {
                    "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "24", "36"
            },
            optionNames = {
                    "1个月", "2个月", "3个月", "4个月", "5个月", "6个月", "7个月", "8个月", "9个月", "10个月", "11个月",
                    "1年", "2年", "3年"
            },
            conditions = "this.chargeType === 'BANDWIDTH_PREPAID_BY_MONTH'"
    )
    private String prepaidPeriod;
    @SelectField(
            label = "自动续费方式",
            options = {
                    "0",
                    "1",
                    "2",
            },
            optionNames = {
                    "手动续费",
                    "自动续费",
                    "到期不续费"
            },
            defaultValues = "0",
            conditions = "this.chargeType === 'BANDWIDTH_PREPAID_BY_MONTH'"
    )
    private String renewFlag;

    @NumberField(
            label = "出带宽上限",
            min = 1
    )
    private Long internetMaxBandwidthOut;

//    @SelectField(
//            label = "弹性IP类型",
//            options = {
//                    "EIP",
//                    "AnycastEIP",
//                    "HighQualityEIP",
//                    "AntiDDoSEIP"
//            },
//            optionNames = {
//                    "默认",
//                    "加速IP",
//                    "精品IP",
//                    "高防IP"
//            }
//    )
//    private String addressType;
//
//    @SelectField(
//            label = "Anycast发布域",
//            options = {
//                    "ANYCAST_ZONE_GLOBAL",
//                    "ANYCAST_ZONE_OVERSEAS",
//            },
//            optionNames = {
//                    "全球发布域",
//                    "境外发布域",
//            }
//    )
//    private String anycastZone;
}
