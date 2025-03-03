package com.stratocloud.provider.aliyun.lb.classic.actions;

import com.stratocloud.form.InputField;
import com.stratocloud.form.NumberField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class AliyunClbBuildInput implements ResourceActionInput {
    @SelectField(
            label = "计费方式",
            options = {"PayBySpec", "PayByCLCU"},
            optionNames = {"按规格计费", "按使用量计费"},
            defaultValues = "PayBySpec"
    )
    private String instanceChargeType;

    @SelectField(
            label = "计量方式",
            options = {
                    "paybytraffic", "paybybandwidth"
            },
            optionNames = {
                    "按流量计费", "按带宽计费"
            },
            defaultValues = "paybytraffic",
            conditions = "this.instanceChargeType === 'PayBySpec'"
    )
    private String internetChargeType;

    @NumberField(
            label = "带宽峰值",
            min = 1,
            max = 5120,
            conditions = "this.internetChargeType === 'paybybandwidth'"
    )
    private Integer bandwidth;

    @SelectField(
            label = "规格",
            options = {
                    "slb.s1.small",
                    "slb.s2.small",
                    "slb.s2.medium",
                    "slb.s3.small",
                    "slb.s3.medium",
                    "slb.s3.large"
            },
            optionNames = {
                    "slb.s1.small",
                    "slb.s2.small",
                    "slb.s2.medium",
                    "slb.s3.small",
                    "slb.s3.medium",
                    "slb.s3.large"
            },
            conditions = "this.instanceChargeType === 'PayBySpec'"
    )
    private String spec;

    @SelectField(
            label = "IP 版本",
            options = {"ipv4", "ipv6"},
            optionNames = {"IPv4", "IPv6"},
            defaultValues = "ipv4"
    )
    private String ipVersion;

    @InputField(label = "备可用区ID", required = false)
    private String slaveZoneId;
}
