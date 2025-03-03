package com.stratocloud.limit.jobs;

import com.stratocloud.constant.StratoServices;
import com.stratocloud.job.AsyncJob;
import com.stratocloud.job.AsyncJobContext;
import com.stratocloud.job.AsyncJobHandler;
import com.stratocloud.job.JobScheduler;
import com.stratocloud.limit.ResourceUsageLimitService;
import com.stratocloud.permission.DynamicPermissionRequired;
import com.stratocloud.permission.PermissionItem;
import com.stratocloud.resource.ResourcePermissionTarget;
import org.springframework.stereotype.Component;

@Component
public class SynchronizeLimitsJobHandler
        implements AsyncJobHandler<SynchronizeAllLimitsCmd>, DynamicPermissionRequired {

    private final ResourceUsageLimitService resourceUsageLimitService;

    private final SynchronizeLimitsJobScheduler scheduler;

    public SynchronizeLimitsJobHandler(ResourceUsageLimitService resourceUsageLimitService,
                                       SynchronizeLimitsJobScheduler scheduler) {
        this.resourceUsageLimitService = resourceUsageLimitService;
        this.scheduler = scheduler;
    }

    @Override
    public String getJobType() {
        return "SYNCHRONIZE_RESOURCE_USAGE_LIMITS";
    }

    @Override
    public String getJobTypeName() {
        return "同步所有配额";
    }

    @Override
    public String getStartJobTopic() {
        return "SYNCHRONIZE_RESOURCE_USAGE_LIMITS_JOB_START";
    }

    @Override
    public String getCancelJobTopic() {
        return "SYNCHRONIZE_RESOURCE_USAGE_LIMITS_JOB_CANCEL";
    }

    @Override
    public String getServiceName() {
        return StratoServices.RESOURCE_SERVICE;
    }

    @Override
    public boolean isTransientJob() {
        return false;
    }

    @Override
    public boolean defaultWorkflowRequireOrder() {
        return false;
    }

    @Override
    public void preCreateJob(SynchronizeAllLimitsCmd parameters) {
        resourceUsageLimitService.synchronizeAllLimits();
    }

    @Override
    public void onUpdateJob(SynchronizeAllLimitsCmd parameters) {
        AsyncJob asyncJob = AsyncJobContext.current().getAsyncJob();

        asyncJob.discardCurrentExecutions();

        resourceUsageLimitService.synchronizeAllLimits();
    }

    @Override
    public void onCancelJob(String message, SynchronizeAllLimitsCmd parameters) {
        AsyncJob asyncJob = AsyncJobContext.current().getAsyncJob();
        asyncJob.cancel(message);
    }

    @Override
    public void onStartJob(SynchronizeAllLimitsCmd parameters) {
        AsyncJob asyncJob = AsyncJobContext.current().getAsyncJob();
        asyncJob.start();
    }

    @Override
    public PermissionItem getPermissionItem() {
        return new PermissionItem(
                ResourcePermissionTarget.ID, ResourcePermissionTarget.NAME, getJobType(), getJobTypeName()
        );
    }


    @Override
    public JobScheduler getScheduler() {
        return scheduler;
    }
}
