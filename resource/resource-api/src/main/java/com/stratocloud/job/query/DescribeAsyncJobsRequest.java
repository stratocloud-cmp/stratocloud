package com.stratocloud.job.query;

import com.stratocloud.request.query.PagingRequest;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DescribeAsyncJobsRequest extends PagingRequest {
    private List<Long> jobIds;


}
