package com.stratocloud.workflow;

import com.stratocloud.constant.StratoServices;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface WorkflowApi {
    @PostMapping(path = StratoServices.ORDER_SERVICE+"/create-workflow")
    CreateWorkflowResponse createWorkflow(@RequestBody CreateWorkflowCmd cmd);
    @PostMapping(path = StratoServices.ORDER_SERVICE+"/update-workflow")
    UpdateWorkflowResponse updateWorkflow(@RequestBody UpdateWorkflowCmd cmd);

    @PostMapping(path = StratoServices.ORDER_SERVICE+"/delete-workflows")
    DeleteWorkflowsResponse deleteWorkflows(@RequestBody DeleteWorkflowsCmd cmd);

    @PostMapping(path = StratoServices.ORDER_SERVICE+"/describe-workflows")
    Page<NestedWorkflowResponse> describeWorkflows(@RequestBody DescribeWorkflowsRequest request);

    @PostMapping(path = StratoServices.ORDER_SERVICE+"/describe-node-types")
    DescribeNodeTypesResponse describeNodeTypes(@RequestBody DescribeNodeTypesRequest request);
}
