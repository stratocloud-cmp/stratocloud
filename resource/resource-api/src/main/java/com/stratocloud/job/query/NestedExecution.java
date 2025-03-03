package com.stratocloud.job.query;

import com.stratocloud.request.query.NestedAuditable;
import com.stratocloud.job.ExecutionState;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class NestedExecution extends NestedAuditable {
    private ExecutionState state;
    private List<NestedExecutionStep> steps;
}
