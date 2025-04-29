package com.stratocloud.provider.aliyun.snapshot.actions;

import com.stratocloud.form.NumberField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class AliyunSnapshotBuildInput implements ResourceActionInput {
    @NumberField(label = "保留天数", defaultValue = 30)
    private Integer retentionDays;
}
