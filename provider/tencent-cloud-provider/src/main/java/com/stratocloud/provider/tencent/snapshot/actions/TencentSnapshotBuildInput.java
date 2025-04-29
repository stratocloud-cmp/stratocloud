package com.stratocloud.provider.tencent.snapshot.actions;

import com.stratocloud.form.NumberField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class TencentSnapshotBuildInput implements ResourceActionInput {
    @NumberField(label = "保留天数", defaultValue = 30)
    private Integer retentionDays;
}
