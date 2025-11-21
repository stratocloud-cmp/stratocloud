package com.stratocloud.provider.huawei.dcs.actions;

import com.stratocloud.form.DynamicForm;
import com.stratocloud.form.NumberField;
import com.stratocloud.form.SelectField;
import lombok.Data;

import java.util.List;

@Data
public class BackupPolicyInput implements DynamicForm {
    @SelectField(
            label = "备份类型",
            options = {
                    "auto",
                    "manual"
            },
            optionNames = {
                    "自动备份",
                    "手动备份"
            },
            defaultValues = "auto"
    )
    private String backupType;

    @NumberField(label = "保留天数", min = 1, max = 7, defaultValue = 3, conditions = "this.backupType === 'auto'")
    private Integer saveDays;

    @SelectField(
            label = "备份周期",
            options = {
                    "1", "2", "3", "4", "5", "6", "7"
            },
            optionNames = {
                    "星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"
            },
            defaultValues = {"1", "2", "3", "4", "5", "6", "7"},
            conditions = "this.backupType === 'auto'"
    )
    private List<Long> backupAt;

    @SelectField(
            label = "备份执行时间(时长1小时)",
            options = {
                    "00:00","01:00","02:00","03:00","04:00","05:00","06:00","07:00",
                    "08:00","09:00","10:00","11:00","12:00","13:00","14:00","15:00",
                    "16:00","17:00","18:00","19:00","20:00","21:00","22:00","23:00"
            },
            optionNames = {
                    "00:00","01:00","02:00","03:00","04:00","05:00","06:00","07:00",
                    "08:00","09:00","10:00","11:00","12:00","13:00","14:00","15:00",
                    "16:00","17:00","18:00","19:00","20:00","21:00","22:00","23:00"
            },
            defaultValues = "00:00",
            conditions = "this.backupType === 'auto'"
    )
    private String beginAt;
}
