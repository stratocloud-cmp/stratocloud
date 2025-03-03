package com.stratocloud.job.query;

import com.stratocloud.request.query.NestedControllable;
import com.stratocloud.job.JobStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
public class NestedJobResponse extends NestedControllable {
    private Boolean manualStart;
    private LocalDateTime plannedStartTime;
    private JobStatus status;
    private Map<String, Object> parameters;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private String errorMessage;

    private String jobType;
    private String jobTypeName;
}
