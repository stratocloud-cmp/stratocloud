package com.stratocloud.job.query;

import com.stratocloud.request.query.NestedControllable;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class NestedAsyncJobResponse extends NestedControllable {
    private String jobType;
    private Boolean ended;
    private List<NestedExecution> executions;
}
