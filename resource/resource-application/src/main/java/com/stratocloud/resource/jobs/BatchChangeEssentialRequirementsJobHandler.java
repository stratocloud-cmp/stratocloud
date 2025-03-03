package com.stratocloud.resource.jobs;

import com.stratocloud.constant.StratoServices;
import com.stratocloud.job.AsyncJob;
import com.stratocloud.job.AsyncJobContext;
import com.stratocloud.job.AsyncJobHandler;
import com.stratocloud.resource.ResourceService;
import com.stratocloud.resource.cmd.relationship.BatchChangeEssentialRequirementsCmd;
import org.springframework.stereotype.Component;

@Component
public class BatchChangeEssentialRequirementsJobHandler implements AsyncJobHandler<BatchChangeEssentialRequirementsCmd> {
    private final ResourceService resourceService;

    public BatchChangeEssentialRequirementsJobHandler(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @Override
    public String getJobType() {
        return "BATCH_CHANGE_ESSENTIAL_REQUIREMENTS";
    }

    @Override
    public String getJobTypeName() {
        return "变更依赖资源";
    }

    @Override
    public String getStartJobTopic() {
        return "BATCH_CHANGE_ESSENTIAL_REQUIREMENTS_JOB_START";
    }

    @Override
    public String getCancelJobTopic() {
        return "BATCH_CHANGE_ESSENTIAL_REQUIREMENTS_JOB_CANCEL";
    }

    @Override
    public String getServiceName() {
        return StratoServices.RESOURCE_SERVICE;
    }

    @Override
    public void preCreateJob(BatchChangeEssentialRequirementsCmd parameters) {
        resourceService.changeEssentialRequirements(parameters);
    }

    @Override
    public void onUpdateJob(BatchChangeEssentialRequirementsCmd parameters) {
        AsyncJob asyncJob = AsyncJobContext.current().getAsyncJob();

        asyncJob.discardCurrentExecutions();

        resourceService.changeEssentialRequirements(parameters);
    }

    @Override
    public void onCancelJob(String message, BatchChangeEssentialRequirementsCmd parameters) {
        AsyncJob asyncJob = AsyncJobContext.current().getAsyncJob();
        asyncJob.cancel(message);
    }

    @Override
    public void onStartJob(BatchChangeEssentialRequirementsCmd parameters) {
        AsyncJob asyncJob = AsyncJobContext.current().getAsyncJob();
        asyncJob.start();
    }
}
