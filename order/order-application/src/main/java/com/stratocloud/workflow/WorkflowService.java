package com.stratocloud.workflow;

import com.stratocloud.workflow.cmd.CreateWorkflowCmd;
import com.stratocloud.workflow.cmd.DeleteWorkflowsCmd;
import com.stratocloud.workflow.cmd.UpdateWorkflowCmd;
import com.stratocloud.workflow.query.DescribeNodeTypesRequest;
import com.stratocloud.workflow.query.DescribeNodeTypesResponse;
import com.stratocloud.workflow.query.DescribeWorkflowsRequest;
import com.stratocloud.workflow.query.NestedWorkflowResponse;
import com.stratocloud.workflow.response.CreateWorkflowResponse;
import com.stratocloud.workflow.response.DeleteWorkflowsResponse;
import com.stratocloud.workflow.response.UpdateWorkflowResponse;
import org.springframework.data.domain.Page;

public interface WorkflowService {
    CreateWorkflowResponse createWorkflow(CreateWorkflowCmd cmd);

    UpdateWorkflowResponse updateWorkflow(UpdateWorkflowCmd cmd);

    DeleteWorkflowsResponse deleteWorkflows(DeleteWorkflowsCmd cmd);

    Page<NestedWorkflowResponse> describeWorkflows(DescribeWorkflowsRequest request);

    DescribeNodeTypesResponse describeNodeTypes(DescribeNodeTypesRequest request);
}
