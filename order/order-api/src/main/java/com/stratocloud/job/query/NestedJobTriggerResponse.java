package com.stratocloud.job.query;

import com.stratocloud.request.query.NestedEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class NestedJobTriggerResponse implements NestedEntity {
    private String triggerId;

    private String jobType;
    private String jobTypeName;

    private String cronExpression;
    private LocalDateTime nextTriggerTime;
    private Boolean disabled;
    private String description;
}
