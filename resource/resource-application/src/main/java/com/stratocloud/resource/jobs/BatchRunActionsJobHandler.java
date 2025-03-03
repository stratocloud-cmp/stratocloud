package com.stratocloud.resource.jobs;

import com.stratocloud.constant.StratoServices;
import com.stratocloud.job.AsyncJob;
import com.stratocloud.job.AsyncJobContext;
import com.stratocloud.job.AsyncJobHandler;
import com.stratocloud.resource.ResourceService;
import com.stratocloud.resource.cmd.BatchRunActionsCmd;
import org.springframework.stereotype.Component;

@Component
public class BatchRunActionsJobHandler implements AsyncJobHandler<BatchRunActionsCmd> {

    private final ResourceService resourceService;

    public BatchRunActionsJobHandler(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @Override
    public String getJobType() {
        return "BATCH_RUN_RESOURCE_ACTIONS";
    }

    @Override
    public String getJobTypeName() {
        return "执行资源操作";
    }

    @Override
    public String getStartJobTopic() {
        return "BATCH_RUN_RESOURCE_ACTIONS_JOB_START";
    }

    @Override
    public String getCancelJobTopic() {
        return "BATCH_RUN_RESOURCE_ACTIONS_JOB_CANCEL";
    }

    @Override
    public String getServiceName() {
        return StratoServices.RESOURCE_SERVICE;
    }

    @Override
    public void preCreateJob(BatchRunActionsCmd parameters) {
        resourceService.runActions(parameters);
    }

    @Override
    public void onUpdateJob(BatchRunActionsCmd parameters) {
        AsyncJob asyncJob = AsyncJobContext.current().getAsyncJob();

        asyncJob.discardCurrentExecutions();

        resourceService.runActions(parameters);
    }

    @Override
    public void onCancelJob(String message, BatchRunActionsCmd parameters) {
        AsyncJob asyncJob = AsyncJobContext.current().getAsyncJob();
        asyncJob.cancel(message);
    }

    @Override
    public void onStartJob(BatchRunActionsCmd parameters) {
        AsyncJob asyncJob = AsyncJobContext.current().getAsyncJob();
        asyncJob.start();
    }
}
