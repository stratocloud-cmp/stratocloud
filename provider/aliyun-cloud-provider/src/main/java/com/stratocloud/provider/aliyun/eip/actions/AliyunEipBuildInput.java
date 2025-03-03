package com.stratocloud.provider.aliyun.eip.actions;

import com.stratocloud.form.NumberField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

import java.util.List;

@Data
public class AliyunEipBuildInput implements ResourceActionInput {
    @SelectField(
            label = "线路类型",
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
                    "PrePaid",
                    "PostPaid"
            },
            optionNames = {
                    "包年包月",
                    "按量计费"
            },
            defaultValues = "PostPaid"
    )
    private String instanceChargeType;

    @SelectField(
            label = "计量方式",
            options = {
                    "PayByBandwidth",
                    "PayByTraffic"
            },
            optionNames = {
                    "按带宽计费",
                    "按流量计费"
            },
            defaultValues = "PayByBandwidth"
    )
    private String internetChargeType;
    @SelectField(
            label = "购买时长",
            options = {
                    "1", "2", "3", "4", "5", "6", "7", "8", "9", "12", "24", "36", "48", "60"
            },
            optionNames = {
                    "1个月", "2个月", "3个月", "4个月", "5个月", "6个月", "7个月", "8个月", "9个月",
                    "1年", "2年", "3年", "4年", "5年"
            },
            conditions = "this.instanceChargeType === 'PrePaid'"
    )
    private Integer prepaidPeriod;

    @NumberField(
            label = "带宽峰值(Mbps)",
            min = 1,
            defaultValue = 5
    )
    private Long bandwidth;


    @SelectField(
            label = "额外安全防护级别",
            options = {
                    "AntiDDoS_Enhanced"
            },
            optionNames = {
                    "DDoS 防护（增强版）"
            },
            multiSelect = true,
            allowCreate = true,
            required = false,
            description = "留空时，默认为 DDoS 防护（基础版）"
    )
    private List<String> securityProtectionTypes;
}
