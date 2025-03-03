package com.stratocloud.resource.jobs;

import com.stratocloud.constant.StratoServices;
import com.stratocloud.job.AsyncJob;
import com.stratocloud.job.AsyncJobContext;
import com.stratocloud.job.AsyncJobHandler;
import com.stratocloud.job.JobScheduler;
import com.stratocloud.permission.DynamicPermissionRequired;
import com.stratocloud.permission.PermissionItem;
import com.stratocloud.resource.ResourcePermissionTarget;
import com.stratocloud.resource.ResourceService;
import com.stratocloud.resource.cmd.SynchronizeResourcesCmd;
import org.springframework.stereotype.Component;

@Component
public class SynchronizeResourcesJobHandler
        implements AsyncJobHandler<SynchronizeResourcesCmd>, DynamicPermissionRequired {

    private final ResourceService resourceService;

    private final SynchronizeResourcesJobScheduler scheduler;

    public SynchronizeResourcesJobHandler(ResourceService resourceService,
                                          SynchronizeResourcesJobScheduler scheduler) {
        this.resourceService = resourceService;
        this.scheduler = scheduler;
    }

    @Override
    public String getJobType() {
        return "SYNCHRONIZE_RESOURCES";
    }

    @Override
    public String getJobTypeName() {
        return "同步云资源";
    }

    @Override
    public String getStartJobTopic() {
        return "SYNCHRONIZE_RESOURCES_JOB_START";
    }

    @Override
    public String getCancelJobTopic() {
        return "SYNCHRONIZE_RESOURCES_JOB_CANCEL";
    }

    @Override
    public String getServiceName() {
        return StratoServices.RESOURCE_SERVICE;
    }

    @Override
    public boolean isTransientJob() {
        return true;
    }

    @Override
    public boolean defaultWorkflowRequireOrder() {
        return false;
    }

    @Override
    public void preCreateJob(SynchronizeResourcesCmd parameters) {
        resourceService.synchronizeResources(parameters);
    }

    @Override
    public void onUpdateJob(SynchronizeResourcesCmd parameters) {
        AsyncJob asyncJob = AsyncJobContext.current().getAsyncJob();

        asyncJob.discardCurrentExecutions();

        resourceService.synchronizeResources(parameters);
    }

    @Override
    public void onCancelJob(String message, SynchronizeResourcesCmd parameters) {
        AsyncJob asyncJob = AsyncJobContext.current().getAsyncJob();
        asyncJob.cancel(message);
    }

    @Override
    public void onStartJob(SynchronizeResourcesCmd parameters) {
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
