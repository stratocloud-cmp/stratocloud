package com.stratocloud.provider.huawei.elb.actions;

import com.stratocloud.form.BooleanField;
import com.stratocloud.form.IpField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

import java.util.List;

@Data
public class HuaweiLoadBalancerBuildInput implements ResourceActionInput {
    @BooleanField(label = "是否预付费")
    private boolean prepaid;

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
            conditions = "this.prepaid === true"
    )
    private String period;

    @BooleanField(label = "自动续订", conditions = "this.prepaid === true")
    private boolean autoRenew;




    @IpField(label = "指定内网IP", placeHolder = "请选择IP或直接输入, 留空将自动分配IP")
    private List<String> ips;
    @BooleanField(label = "启用IPv6")
    private boolean usingIpv6;
    @BooleanField(label = "启用跨VPC转发")
    private boolean ipTargetEnabled;
}
