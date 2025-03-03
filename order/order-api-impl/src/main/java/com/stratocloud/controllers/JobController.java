package com.stratocloud.controllers;

import com.stratocloud.audit.SendAuditLog;
import com.stratocloud.job.*;
import com.stratocloud.job.cmd.*;
import com.stratocloud.job.query.*;
import com.stratocloud.job.response.*;
import com.stratocloud.permission.*;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@PermissionTarget(target = "Job", targetName = "任务")
@RestController
public class JobController implements JobApi {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @Override
    @SendAuditLog(
            action = "CreateJob",
            actionName = "创建任务",
            objectType = "Job",
            objectTypeName = "任务"
    )
    public CreateJobResponse createJob(@RequestBody CreateJobCmd cmd) {
        return jobService.createJob(cmd);
    }

    @Override
    @ReadPermissionRequired(checkPermission = false)
    public Page<NestedJobResponse> describeJobs(@RequestBody DescribeJobsRequest request) {
        return jobService.describeJobs(request);
    }

    @Override
    @ReadPermissionRequired(checkPermission = false)
    public Page<NestedJobDefinitionResponse> describeJobDefinitions(@RequestBody DescribeJobDefinitionsRequest request) {
        return jobService.describeJobDefinitions(request);
    }

    @Override
    @PermissionRequired(action = "UPDATE_JOB_DEFINITION", actionName = "修改任务类型定义")
    @SendAuditLog(
            action = "ChangeJobDefinitionOrderRequirement",
            actionName = "修改任务工单开关",
            objectType = "JobDefinition",
            objectTypeName = "任务类型定义"
    )
    public ChangeOrderRequirementResponse changeJobDefinitionOrderRequirement(@RequestBody ChangeOrderRequirementCmd cmd) {
        return jobService.changeJobDefinitionOrderRequirement(cmd);
    }


    @Override
    @ReadPermissionRequired
    public Page<NestedJobTriggerResponse> describeJobTriggers(@RequestBody DescribeJobTriggersRequest request) {
        return jobService.describeJobTriggers(request);
    }

    @Override
    @PermissionRequired(action = "UPDATE_JOB_TRIGGER", actionName = "修改定时触发器")
    @SendAuditLog(
            action = "UpdateJobTrigger",
            actionName = "修改定时触发器",
            objectType = "JobTrigger",
            objectTypeName = "定时触发器"
    )
    public UpdateJobTriggerResponse updateJobTrigger(@RequestBody UpdateJobTriggerCmd cmd) {
        return jobService.updateJobTrigger(cmd);
    }


    @Override
    @PermissionRequired(action = "UPDATE_JOB_TRIGGER", actionName = "修改定时触发器")
    @SendAuditLog(
            action = "EnableJobTrigger",
            actionName = "启用定时触发器",
            objectType = "JobTrigger",
            objectTypeName = "定时触发器"
    )
    public EnableJobTriggerResponse enableTrigger(@RequestBody EnableJobTriggerCmd cmd) {
        return jobService.enableTrigger(cmd);
    }

    @Override
    @PermissionRequired(action = "UPDATE_JOB_TRIGGER", actionName = "修改定时触发器")
    @SendAuditLog(
            action = "DisableJobTrigger",
            actionName = "停用定时触发器",
            objectType = "JobTrigger",
            objectTypeName = "定时触发器"
    )
    public DisableJobTriggerResponse disableTrigger(@RequestBody DisableJobTriggerCmd cmd) {
        return jobService.disableTrigger(cmd);
    }

    @Override
    @PermissionRequired(action = "TRIGGER_JOB_ONCE", actionName = "执行一次定时任务")
    @SendAuditLog(
            action = "TriggerJobOnce",
            actionName = "执行一次定时任务",
            objectType = "JobTrigger",
            objectTypeName = "定时触发器"
    )
    public TriggerJobOnceResponse triggerJobOnce(@RequestBody TriggerJobOnceCmd cmd) {
        return jobService.triggerJobOnce(cmd);
    }

    @Override
    @SendAuditLog(
            action = "RetryJob",
            actionName = "重试任务",
            objectType = "Job",
            objectTypeName = "任务"
    )
    public RetryJobResponse retryJob(@RequestBody RetryJobCmd cmd) {
        return jobService.retryJob(cmd);
    }
}
