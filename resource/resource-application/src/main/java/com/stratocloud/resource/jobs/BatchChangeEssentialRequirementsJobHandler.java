package com.stratocloud.resource.jobs;

import com.stratocloud.constant.StratoServices;
import com.stratocloud.job.AsyncJob;
import com.stratocloud.job.AsyncJobContext;
import com.stratocloud.job.AsyncJobHandler;
import com.stratocloud.job.JobContext;
import com.stratocloud.repository.ResourceRepository;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceService;
import com.stratocloud.resource.cmd.relationship.BatchChangeEssentialRequirementsCmd;
import com.stratocloud.resource.cmd.relationship.ChangeEssentialRequirementCmd;
import com.stratocloud.tag.NestedTag;
import com.stratocloud.tag.TagRecord;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class BatchChangeEssentialRequirementsJobHandler implements AsyncJobHandler<BatchChangeEssentialRequirementsCmd> {

    private final ResourceService resourceService;

    private final ResourceRepository resourceRepository;

    public BatchChangeEssentialRequirementsJobHandler(ResourceService resourceService,
                                                      ResourceRepository resourceRepository) {
        this.resourceService = resourceService;
        this.resourceRepository = resourceRepository;
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


    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> prepareRuntimeProperties(BatchChangeEssentialRequirementsCmd jobParameters) {
        List<NestedTag> nestedTags = new ArrayList<>();

        if(Utils.isNotEmpty(jobParameters.getChanges())){
            for (ChangeEssentialRequirementCmd change : jobParameters.getChanges()) {
                Resource resource = resourceRepository.findResource(change.getSourceId());

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
