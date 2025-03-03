package com.stratocloud.controllers;

import com.stratocloud.audit.SendAuditLog;
import com.stratocloud.job.AsyncJobApi;
import com.stratocloud.job.AsyncJobService;
import com.stratocloud.job.cmd.RunAsyncJobCmd;
import com.stratocloud.job.query.DescribeAsyncJobsRequest;
import com.stratocloud.job.query.NestedAsyncJobResponse;
import com.stratocloud.job.response.RunAsyncJobResponse;
import com.stratocloud.permission.PermissionTarget;
import com.stratocloud.permission.ReadPermissionRequired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@PermissionTarget(target = "Job", targetName = "任务")
@RestController
public class AsyncJobController implements AsyncJobApi {

    private final AsyncJobService asyncJobService;

    public AsyncJobController(AsyncJobService asyncJobService) {
        this.asyncJobService = asyncJobService;
    }

    @Override
    @SendAuditLog(
            action = "RunAsyncJob",
            actionName = "执行异步任务",
            objectType = "AsyncJob",
            objectTypeName = "异步任务"
    )
    public RunAsyncJobResponse runAsyncJob(@RequestBody RunAsyncJobCmd cmd) {
        return asyncJobService.runAsyncJob(cmd);
    }


    @Override
    @ReadPermissionRequired(checkPermission = false)
    public Page<NestedAsyncJobResponse> describeAsyncJobs(@RequestBody DescribeAsyncJobsRequest request) {
        return asyncJobService.describeAsyncJobs(request);
    }
}
