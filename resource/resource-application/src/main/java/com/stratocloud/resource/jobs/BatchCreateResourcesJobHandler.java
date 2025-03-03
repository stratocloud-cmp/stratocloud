package com.stratocloud.resource.jobs;

import com.stratocloud.constant.StratoServices;
import com.stratocloud.job.AsyncJob;
import com.stratocloud.job.AsyncJobContext;
import com.stratocloud.job.AsyncJobHandler;
import com.stratocloud.resource.ResourceService;
import com.stratocloud.resource.cmd.BatchCreateResourcesCmd;
import org.springframework.stereotype.Component;

@Component
public class BatchCreateResourcesJobHandler implements AsyncJobHandler<BatchCreateResourcesCmd> {

    private final ResourceService resourceService;

    public BatchCreateResourcesJobHandler(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @Override
    public String getJobType() {
        return "BATCH_CREATE_RESOURCES";
    }

    @Override
    public String getJobTypeName() {
        return "创建资源";
    }

    @Override
    public String getStartJobTopic() {
        return "BATCH_CREATE_RESOURCES_JOB_START";
    }

    @Override
    public String getCancelJobTopic() {
        return "BATCH_CREATE_RESOURCES_JOB_CANCEL";
    }

    @Override
    public String getServiceName() {
        return StratoServices.RESOURCE_SERVICE;
    }

    @Override
    public void preCreateJob(BatchCreateResourcesCmd parameters) {
        resourceService.create(parameters);
    }

    @Override
    public void onUpdateJob(BatchCreateResourcesCmd parameters) {
        AsyncJob asyncJob = AsyncJobContext.current().getAsyncJob();

        asyncJob.discardCurrentExecutions();

        resourceService.create(parameters);
    }

    @Override
    public void onCancelJob(String message, BatchCreateResourcesCmd parameters) {
        AsyncJob asyncJob = AsyncJobContext.current().getAsyncJob();
        asyncJob.cancel(message);
    }

    @Override
    public void onStartJob(BatchCreateResourcesCmd parameters) {
        AsyncJob asyncJob = AsyncJobContext.current().getAsyncJob();
        asyncJob.start();
    }
}
