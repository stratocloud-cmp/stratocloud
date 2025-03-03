package com.stratocloud.resource.jobs;

import com.stratocloud.constant.StratoServices;
import com.stratocloud.job.AsyncJob;
import com.stratocloud.job.AsyncJobContext;
import com.stratocloud.job.AsyncJobHandler;
import com.stratocloud.resource.ResourceService;
import com.stratocloud.resource.cmd.relationship.BatchConnectResourcesCmd;
import org.springframework.stereotype.Component;

@Component
public class BatchConnectResourcesJobHandler implements AsyncJobHandler<BatchConnectResourcesCmd> {
    private final ResourceService resourceService;

    public BatchConnectResourcesJobHandler(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @Override
    public String getJobType() {
        return "BATCH_CONNECT_RESOURCES";
    }

    @Override
    public String getJobTypeName() {
        return "挂载资源";
    }

    @Override
    public String getStartJobTopic() {
        return "BATCH_CONNECT_RESOURCES_JOB_START";
    }

    @Override
    public String getCancelJobTopic() {
        return "BATCH_CONNECT_RESOURCES_JOB_CANCEL";
    }

    @Override
    public String getServiceName() {
        return StratoServices.RESOURCE_SERVICE;
    }

    @Override
    public void preCreateJob(BatchConnectResourcesCmd parameters) {
        resourceService.connectResources(parameters);
    }

    @Override
    public void onUpdateJob(BatchConnectResourcesCmd parameters) {
        AsyncJob asyncJob = AsyncJobContext.current().getAsyncJob();

        asyncJob.discardCurrentExecutions();

        resourceService.connectResources(parameters);
    }

    @Override
    public void onCancelJob(String message, BatchConnectResourcesCmd parameters) {
        AsyncJob asyncJob = AsyncJobContext.current().getAsyncJob();
        asyncJob.cancel(message);
    }

    @Override
    public void onStartJob(BatchConnectResourcesCmd parameters) {
        AsyncJob asyncJob = AsyncJobContext.current().getAsyncJob();
        asyncJob.start();
    }
}
