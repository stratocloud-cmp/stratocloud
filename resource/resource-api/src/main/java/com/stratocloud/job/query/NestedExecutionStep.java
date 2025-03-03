package com.stratocloud.job.query;

import com.stratocloud.request.query.NestedAuditable;
import com.stratocloud.job.ExecutionStepState;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class NestedExecutionStep extends NestedAuditable {
    private Integer stepIndex;
    private ExecutionStepState state;
    private List<NestedTask> tasks;
}
