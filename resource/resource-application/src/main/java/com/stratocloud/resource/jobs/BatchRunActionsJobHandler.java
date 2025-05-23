package com.stratocloud.resource.jobs;

import com.stratocloud.constant.StratoServices;
import com.stratocloud.job.AsyncJob;
import com.stratocloud.job.AsyncJobContext;
import com.stratocloud.job.AsyncJobHandler;
import com.stratocloud.job.JobContext;
import com.stratocloud.repository.ResourceRepository;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceService;
import com.stratocloud.resource.cmd.BatchRunActionsCmd;
import com.stratocloud.resource.cmd.action.RunActionCmd;
import com.stratocloud.tag.NestedTag;
import com.stratocloud.tag.TagRecord;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class BatchRunActionsJobHandler implements AsyncJobHandler<BatchRunActionsCmd> {

    private final ResourceService resourceService;

    private final ResourceRepository resourceRepository;

    public BatchRunActionsJobHandler(ResourceService resourceService,
                                     ResourceRepository resourceRepository) {
        this.resourceService = resourceService;
        this.resourceRepository = resourceRepository;
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


    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> prepareRuntimeProperties(BatchRunActionsCmd jobParameters) {
        List<NestedTag> nestedTags = new ArrayList<>();

        if(Utils.isNotEmpty(jobParameters.getActions())){
            for (RunActionCmd runActionCmd : jobParameters.getActions()) {
                Resource resource = resourceRepository.findResource(runActionCmd.getResourceId());

                if(Utils.isNotEmpty(resource.getTags()))
                    nestedTags.addAll(resource.getTags());
            }
        }

        return Map.of(
                JobContext.KEY_RELATED_TAGS,
                TagRecord.fromNestedTags(nestedTags)
        );
    }
}
