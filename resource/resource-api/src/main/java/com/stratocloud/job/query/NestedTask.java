package com.stratocloud.job.query;

import com.stratocloud.request.query.NestedAuditable;
import com.stratocloud.job.TaskState;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class NestedTask extends NestedAuditable {
    private String name;
    private TaskState state;
    private String entityClass;
    private Long entityId;
    private String entityDescription;
    private String type;
    private String typeName;
    private Map<String, Object> taskInputs;
    private String taskInputsClass;
    private String message;
}
