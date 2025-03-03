package com.stratocloud.workflow.query;

import com.stratocloud.request.query.PagingRequest;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DescribeWorkflowsRequest extends PagingRequest {
    private List<Long> workflowIds;
    private Boolean isReplica = false;
    private String search;
}
