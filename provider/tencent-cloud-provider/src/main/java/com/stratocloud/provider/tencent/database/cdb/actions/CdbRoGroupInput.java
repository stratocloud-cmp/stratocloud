package com.stratocloud.provider.tencent.database.cdb.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.stratocloud.form.*;
import com.tencentcloudapi.cdb.v20170320.models.RoGroup;
import lombok.Data;

import java.util.Objects;

@Data
public class CdbRoGroupInput implements DynamicForm {
    @SelectField(
            label = "指定RO组",
            options = {
                    "alone",
                    "allinone",
                    "join"
            },
            optionNames = {
                    "系统自动分配",
                    "新建RO组",
                    "使用现有RO组"
            }
    )
    private String roGroupMode;

    @InputField(label = "现有RO组ID", conditions = "this.roGroupMode === 'join'")
    private String roGroupId;

    @InputField(label = "RO组名称", conditions = "this.roGroupMode === 'allinone'", required = false)
    private String roGroupName;

    @BooleanField(label = "延迟超限剔除", conditions = "this.roGroupMode === 'allinone'")
    private boolean offlineDelay;

    @NumberField(
            label = "延迟阈值(秒)",
            conditions = "this.roGroupMode === 'allinone' && this.offlineDelay === true",
            defaultValue = 10
    )
    private Long maxDelayTime;

    @NumberField(
            label = "最少实例保留个数",
            min = 1,
            max = 5,
            conditions = "this.roGroupMode === 'allinone' && this.offlineDelay === true",
            defaultValue = 1
    )
    private Long minRoGroupSize;


    @JsonIgnore
    public RoGroup toRoGroup() {
        RoGroup roGroup = new RoGroup();

        roGroup.setRoGroupMode(roGroupMode);

        if(Objects.equals(roGroupMode, "allinone")){
            roGroup.setRoGroupName(roGroupName);
            roGroup.setRoOfflineDelay(offlineDelay ? 1L : 0L);
            roGroup.setRoMaxDelayTime(maxDelayTime);
            roGroup.setMinRoInGroup(minRoGroupSize);
            roGroup.setWeightMode("system");
        }else if(Objects.equals(roGroupMode, "join")){
            roGroup.setRoGroupId(roGroupId);
        }

        return roGroup;
    }
}
