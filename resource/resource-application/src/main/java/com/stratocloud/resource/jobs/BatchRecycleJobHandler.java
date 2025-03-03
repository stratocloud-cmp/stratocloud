package com.stratocloud.resource.jobs;

import com.stratocloud.constant.StratoServices;
import com.stratocloud.job.AsyncJob;
import com.stratocloud.job.AsyncJobContext;
import com.stratocloud.job.AsyncJobHandler;
import com.stratocloud.permission.DynamicPermissionRequired;
import com.stratocloud.permission.PermissionItem;
import com.stratocloud.resource.ResourcePermissionTarget;
import com.stratocloud.resource.ResourceService;
import com.stratocloud.resource.cmd.BatchRecycleCmd;
import org.springframework.stereotype.Component;

@Component
public class BatchRecycleJobHandler implements AsyncJobHandler<BatchRecycleCmd>, DynamicPermissionRequired {

    private final ResourceService resourceService;

    public BatchRecycleJobHandler(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @Override
    public String getJobType() {
        return "BATCH_RECYCLE_RESOURCES";
    }

    @Override
    public String getJobTypeName() {
        return "回收资源";
    }

    @Override
    public String getStartJobTopic() {
        return "BATCH_RECYCLE_RESOURCES_JOB_START";
    }

    @Override
    public String getCancelJobTopic() {
        return "BATCH_RECYCLE_RESOURCES_JOB_CANCEL";
    }

    @Override
    public String getServiceName() {
        return StratoServices.RESOURCE_SERVICE;
    }

    @Override
    public void preCreateJob(BatchRecycleCmd parameters) {
        resourceService.recycle(parameters);
    }

    @Override
    public void onUpdateJob(BatchRecycleCmd parameters) {
        AsyncJob asyncJob = AsyncJobContext.current().getAsyncJob();

        asyncJob.discardCurrentExecutions();

        resourceService.recycle(parameters);
    }

    @Override
    public void onCancelJob(String message, BatchRecycleCmd parameters) {
        AsyncJob asyncJob = AsyncJobContext.current().getAsyncJob();
        asyncJob.cancel(message);
    }

    @Override
    public void onStartJob(BatchRecycleCmd parameters) {
        AsyncJob asyncJob = AsyncJobContext.current().getAsyncJob();
        asyncJob.start();
    }

    @Override
    public PermissionItem getPermissionItem() {
        return new PermissionItem(
                ResourcePermissionTarget.ID, ResourcePermissionTarget.NAME, getJobType(), getJobTypeName()
        );
    }
}
