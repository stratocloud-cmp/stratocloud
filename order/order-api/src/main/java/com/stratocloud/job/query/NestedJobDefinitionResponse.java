package com.stratocloud.job.query;

import com.stratocloud.request.query.NestedEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NestedJobDefinitionResponse implements NestedEntity {
    private String jobType;
    private String jobTypeName;
    private String startJobTopic;
    private String cancelJobTopic;
    private String serviceName;
    private Long defaultWorkflowId;
    private String defaultWorkflowName;
    private Boolean defaultWorkflowRequireOrder;
}
