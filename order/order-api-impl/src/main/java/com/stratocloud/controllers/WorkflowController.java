package com.stratocloud.controllers;

import com.stratocloud.audit.SendAuditLog;
import com.stratocloud.permission.*;
import com.stratocloud.workflow.WorkflowApi;
import com.stratocloud.workflow.WorkflowService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@PermissionTarget(target = "Workflow", targetName = "流程管理")
@RestController
public class WorkflowController implements WorkflowApi {

    private final WorkflowService service;

    public WorkflowController(WorkflowService service) {
        this.service = service;
    }

    @Override
    @CreatePermissionRequired
    @SendAuditLog(
            action = "CreateWorkflow",
            actionName = "创建流程",
            objectType = "Workflow",
            objectTypeName = "流程"
    )
    public CreateWorkflowResponse createWorkflow(@RequestBody CreateWorkflowCmd cmd) {
        return service.createWorkflow(cmd);
    }

    @Override
    @UpdatePermissionRequired
    @SendAuditLog(
            action = "UpdateWorkflow",
            actionName = "更新流程",
            objectType = "Workflow",
            objectTypeName = "流程"
    )
    public UpdateWorkflowResponse updateWorkflow(@RequestBody UpdateWorkflowCmd cmd) {
        return service.updateWorkflow(cmd);
    }

    @Override
    @DeletePermissionRequired
    @SendAuditLog(
            action = "DeleteWorkflows",
            actionName = "删除流程",
            objectType = "Workflow",
            objectTypeName = "流程"
    )
    public DeleteWorkflowsResponse deleteWorkflows(@RequestBody DeleteWorkflowsCmd cmd) {
        return service.deleteWorkflows(cmd);
    }

    @Override
    @ReadPermissionRequired(checkPermission = false)
    public Page<NestedWorkflowResponse> describeWorkflows(@RequestBody DescribeWorkflowsRequest request) {
        return service.describeWorkflows(request);
    }


    @Override
    @ReadPermissionRequired(checkPermission = false)
    public DescribeNodeTypesResponse describeNodeTypes(@RequestBody DescribeNodeTypesRequest request) {
        return service.describeNodeTypes(request);
    }
}
