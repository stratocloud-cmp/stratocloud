package com.stratocloud.resource.jobs;

import com.stratocloud.constant.StratoServices;
import com.stratocloud.job.AsyncJob;
import com.stratocloud.job.AsyncJobContext;
import com.stratocloud.job.AsyncJobHandler;
import com.stratocloud.permission.DynamicPermissionRequired;
import com.stratocloud.permission.PermissionItem;
import com.stratocloud.resource.ResourcePermissionTarget;
import com.stratocloud.resource.ResourceService;
import com.stratocloud.resource.cmd.BatchRestoreCmd;
import org.springframework.stereotype.Component;

@Component
public class BatchRestoreJobHandler implements AsyncJobHandler<BatchRestoreCmd>, DynamicPermissionRequired {

    private final ResourceService resourceService;

    public BatchRestoreJobHandler(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @Override
    public String getJobType() {
        return "BATCH_RESTORE_RESOURCES";
    }

    @Override
    public String getJobTypeName() {
        return "还原资源";
    }

    @Override
    public String getStartJobTopic() {
        return "BATCH_RESTORE_RESOURCES_JOB_START";
    }

    @Override
    public String getCancelJobTopic() {
        return "BATCH_RESTORE_RESOURCES_JOB_CANCEL";
    }

    @Override
    public String getServiceName() {
        return StratoServices.RESOURCE_SERVICE;
    }

    @Override
    public void preCreateJob(BatchRestoreCmd parameters) {
        resourceService.restore(parameters);
    }

    @Override
    public void onUpdateJob(BatchRestoreCmd parameters) {
        AsyncJob asyncJob = AsyncJobContext.current().getAsyncJob();

        asyncJob.discardCurrentExecutions();

        resourceService.restore(parameters);
    }

    @Override
    public void onCancelJob(String message, BatchRestoreCmd parameters) {
        AsyncJob asyncJob = AsyncJobContext.current().getAsyncJob();
        asyncJob.cancel(message);
    }

    @Override
    public void onStartJob(BatchRestoreCmd parameters) {
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
