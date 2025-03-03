package com.stratocloud.provider.tencent.instance.actions;

import com.stratocloud.form.BooleanField;
import com.stratocloud.form.InputField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.tencent.common.TencentInputDescriptions;
import lombok.Data;

@Data
public class TencentInstanceBuildInput implements ResourceActionInput {
    @SelectField(
            label = "计费方式",
            options = {
                    "POSTPAID_BY_HOUR",
                    "PREPAID",
                    "CDHPAID",
                    "SPOTPAID",
                    "CDCPAID"
            },
            optionNames = {
                    "按小时后付费",
                    "预付费",
                    "独享子机",
                    "竞价付费",
                    "独享集群付费"
            },
            defaultValues = "POSTPAID_BY_HOUR"
    )
    private String chargeType;
    @SelectField(
            label = "购买时长",
            options = {
                    "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "24", "36", "48", "60"
            },
            optionNames = {
                    "1个月", "2个月", "3个月", "4个月", "5个月", "6个月", "7个月", "8个月", "9个月", "10个月", "11个月",
                    "1年", "2年", "3年", "4年", "5年"
            },
            conditions = "this.chargeType === 'PREPAID'"
    )
    private String prepaidPeriod;
    @SelectField(
            label = "自动续费方式",
            options = {
                    "NOTIFY_AND_AUTO_RENEW",
                    "NOTIFY_AND_MANUAL_RENEW",
                    "DISABLE_NOTIFY_AND_MANUAL_RENEW",
            },
            optionNames = {
                    "通知过期且自动续费",
                    "通知过期不自动续费",
                    "不通知过期不自动续费"
            },
            conditions = "this.chargeType === 'PREPAID'"
    )
    private String renewType;



    @InputField(label = "操作系统主机名", required = false)
    private String hostName;

    @BooleanField(label = "保存镜像登录设置")
    private Boolean keepImageLogin;
    @InputField(
            label = "初始密码",
            inputType = "password",
            conditions = "this.keepImageLogin!==true",
            required = false,
            description = TencentInputDescriptions.InstancePassword
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

    @BooleanField(
            label = "启用云自动化助手",
            defaultValue = true,
            conditions = "this.enableAdvanceOptions === true"
    )
    private Boolean enableAutomationService;
    @BooleanField(
            label = "启用云安全服务",
            defaultValue = true,
            conditions = "this.enableAdvanceOptions === true"
    )
    private Boolean enableSecurityService;
    @BooleanField(
            label = "启用云监控服务",
            defaultValue = true,
            conditions = "this.enableAdvanceOptions === true"
    )
    private Boolean enableMonitorService;




    @BooleanField(
            label = "开启实例销毁保护",
            description = TencentInputDescriptions.DisableApiTermination,
            conditions = "this.enableAdvanceOptions === true"
    )
    private Boolean disableApiTermination;
}
