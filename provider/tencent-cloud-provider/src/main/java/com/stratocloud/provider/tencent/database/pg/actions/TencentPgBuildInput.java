package com.stratocloud.provider.tencent.database.pg.actions;

import com.stratocloud.form.InputField;
import com.stratocloud.form.NumberField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class TencentPgBuildInput implements ResourceActionInput {
    @SelectField(label = "备可用区", placeholder = "留空表示与主可用区一致",required = false)
    private String slaveZone;

    @SelectField(
            label = "计费类型",
            options = {
                    "PREPAID",
                    "POSTPAID_BY_HOUR"
            },
            optionNames = {
                    "包年包月",
                    "按量计费"
            },
            defaultValues = "POSTPAID_BY_HOUR"
    )
    private String instanceChargeType;
    @SelectField(
            label = "购买时长",
            options = {
                    "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "24", "36"
            },
            optionNames = {
                    "1个月", "2个月", "3个月", "4个月", "5个月", "6个月", "7个月", "8个月", "9个月", "10个月", "11个月",
                    "1年", "2年", "3年"
            },
            conditions = "this.instanceChargeType === 'PREPAID'",
            defaultValues = "1"
    )
    private Long period;
    @SelectField(
            label = "是否自动续费",
            options = {
                    "0",
                    "1"
            },
            optionNames = {
                    "手动续费",
                    "自动续费"
            },
            defaultValues = "1",
            conditions = "this.instanceChargeType === 'PREPAID'"
    )
    private Long autoRenewFlag;
    @SelectField(
            label = "是否自动使用代金券",
            options = {
                    "0",
                    "1"
            },
            optionNames = {
                    "否",
                    "是"
            },
            defaultValues = "0"
    )
    private Long autoVoucher;
    @NumberField(label = "硬盘(GB)", min = 10, step = 10, defaultValue = 100)
    private Long storage;
    @SelectField(
            label = "数据同步方式",
            options = {
                    "Semi-sync",
                    "Async"
            },
            optionNames = {
                    "半同步",
                    "异步"
            },
            defaultValues = "Semi-sync"
    )
    private String syncMode;
    @SelectField(
            label = "字符集",
            options = {
                    "UTF8",
                    "LATIN1"
            },
            optionNames = {
                    "UTF8",
                    "LATIN1"
            },
            defaultValues = "UTF8"
    )
    private String charset;
    @InputField(label = "管理员用户名")
    private String adminName;
    @InputField(label = "管理员密码", inputType = "password")
    private String adminPassword;


}
