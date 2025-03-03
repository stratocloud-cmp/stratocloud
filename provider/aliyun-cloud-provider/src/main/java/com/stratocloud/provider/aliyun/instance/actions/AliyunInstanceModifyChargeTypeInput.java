package com.stratocloud.provider.aliyun.instance.actions;

import com.stratocloud.form.SelectField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class AliyunInstanceModifyChargeTypeInput implements ResourceActionInput {
    @SelectField(
            label = "计费方式",
            options = {
                    "PostPaid",
                    "PrePaid",
            },
            optionNames = {
                    "按量付费",
                    "包年包月",
            },
            defaultValues = "PostPaid"
    )
    private String chargeType;
    @SelectField(
            label = "购买时长",
            options = {
                    "1", "2", "3", "4", "5", "6", "7", "8", "9", "12", "24", "36", "48", "60"
            },
            optionNames = {
                    "1个月", "2个月", "3个月", "4个月", "5个月", "6个月", "7个月", "8个月", "9个月",
                    "1年", "2年", "3年", "4年", "5年"
            },
            defaultValues = "1",
            conditions = "this.chargeType === 'PrePaid'"
    )
    private Integer prepaidPeriod;
}
