package com.stratocloud.provider.huawei.eip.actions;

import com.stratocloud.form.BooleanField;
import com.stratocloud.form.InputField;
import com.stratocloud.form.NumberField;
import com.stratocloud.form.SelectField;
import com.stratocloud.ip.InternetProtocol;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class HuaweiEipBuildInput implements ResourceActionInput {
    @SelectField(
            label = "计费模式",
            options = {
                    "PREPAID_BY_BANDWIDTH",
                    "POSTPAID_BY_TRAFFIC",
                    "POSTPAID_BY_BANDWIDTH"
            },
            optionNames = {
                    "按带宽预付费",
                    "按流量后付费",
                    "按带宽后付费"
            },
            defaultValues = "POSTPAID_BY_BANDWIDTH"
    )
    private HuaweiEipChargeMode chargeMode;

    @SelectField(
            label = "购买时长(月)",
            options = {
                    "1",
                    "2",
                    "3",
                    "4",
                    "5",
                    "6",
                    "7",
                    "8",
                    "9",
                    "12",
                    "24",
                    "36"
            },
            optionNames = {
                    "1个月",
                    "2个月",
                    "3个月",
                    "4个月",
                    "5个月",
                    "6个月",
                    "7个月",
                    "8个月",
                    "9个月",
                    "1年",
                    "2年",
                    "3年"
            },
            defaultValues = "1",
            conditions = "this.chargeMode === 'PREPAID_BY_BANDWIDTH'"
    )
    private String period;

    @BooleanField(label = "自动续订", conditions = "this.chargeMode === 'PREPAID_BY_BANDWIDTH'")
    private boolean autoRenew;

    @SelectField(
            label = "带宽类型",
            options = {
                    "PER", "WHOLE"
            },
            optionNames = {
                    "独占带宽", "共享带宽"
            },
            defaultValues = "PER"
    )
    private HuaweiEipShareType shareType;

    @NumberField(
            label = "带宽大小(Mb/s)",
            min = 1,
            max = 2000,
            conditions = "this.shareType === 'PER'",
            defaultValue = 1
    )
    private Integer bandwidthSize;

    @InputField(label = "带宽ID", conditions = "this.shareType === 'WHOLE'")
    private String bandwidthId;

    @SelectField(
            label = "线路类型",
            options = {
                    "5_bgp",
                    "5_sbgp"
            },
            optionNames = {
                    "全动态BGP",
                    "静态BGP"
            },
            defaultValues = "5_bgp"
    )
    private String eipType;

    @SelectField(
            label = "IP版本",
            options = {
                    "IPv4", "IPv6"
            },
            optionNames = {
                    "IPv4", "IPv6"
            },
            defaultValues = "IPv4"
    )
    private InternetProtocol ipVersion;
}
