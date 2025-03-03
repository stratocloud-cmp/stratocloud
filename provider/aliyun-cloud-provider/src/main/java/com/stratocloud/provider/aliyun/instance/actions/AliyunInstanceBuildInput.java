package com.stratocloud.provider.aliyun.instance.actions;

import com.stratocloud.form.BooleanField;
import com.stratocloud.form.InputField;
import com.stratocloud.form.NumberField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.aliyun.common.AliyunDescriptions;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class AliyunInstanceBuildInput implements ResourceActionInput {
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

    @BooleanField(label = "自动续费", conditions = "this.chargeType === 'PrePaid'")
    private Boolean autoRenew;

    @NumberField(label = "单次自动续费时长", defaultValue = 1, conditions = "this.chargeType === 'PrePaid'")
    private Integer autoRenewPeriod;


    @InputField(label = "操作系统主机名", required = false)
    private String hostName;

    @BooleanField(label = "使用镜像预设密码")
    private Boolean passwordInherit;

    @InputField(
            label = "初始密码",
            inputType = "password",
            conditions = "this.passwordInherit!==true",
            required = false,
            description = AliyunDescriptions.INSTANCE_PASSWORD
    )
    private String password;

    @BooleanField(label = "高级选项")
    private Boolean enableAdvanceOptions;

    @InputField(
            label = "用户数据",
            inputType = "textarea",
            conditions = "this.enableAdvanceOptions === true"
    )
    private String userData;

    @SelectField(
            label = "云安全中心服务",
            options = {"Active", "Deactive"},
            optionNames = {"启用", "不启用"},
            conditions = "this.enableAdvanceOptions === true"
    )
    private String securityEnhancementStrategy;

    @BooleanField(
            label = "开启实例释放保护",
            description = AliyunDescriptions.INSTANCE_DELETION_PROTECTION,
            conditions = "this.enableAdvanceOptions === true"
    )
    private Boolean deletionProtection;
}
