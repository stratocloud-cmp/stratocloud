package com.stratocloud.workflow.factory;

import com.stratocloud.form.SelectEntityType;
import com.stratocloud.form.SelectField;
import com.stratocloud.form.Source;
import com.stratocloud.workflow.NodeProperties;
import lombok.Data;

@Data
public class JobNodeProperties implements NodeProperties {
    @SelectField(
            label = "任务类型",
            source = Source.ENTITY,
            entityType = SelectEntityType.JOB_DEFINITION
    )
    private String jobType;
}
