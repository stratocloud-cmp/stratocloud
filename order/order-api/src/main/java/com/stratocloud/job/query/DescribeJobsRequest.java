package com.stratocloud.job.query;

import com.stratocloud.request.query.PagingRequest;
import com.stratocloud.job.JobStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DescribeJobsRequest extends PagingRequest {
    private List<Long> jobIds;
    private List<Long> tenantIds;
    private List<Long> ownerIds;
    private List<JobStatus> jobStatuses;
    private String search;
}
