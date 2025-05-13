package com.stratocloud.resource.jobs;

import com.stratocloud.constant.StratoServices;
import com.stratocloud.job.AsyncJob;
import com.stratocloud.job.AsyncJobContext;
import com.stratocloud.job.AsyncJobHandler;
import com.stratocloud.job.JobContext;
import com.stratocloud.repository.RelationshipRepository;
import com.stratocloud.resource.Relationship;
import com.stratocloud.resource.ResourceJobHelper;
import com.stratocloud.resource.ResourceService;
import com.stratocloud.resource.cmd.relationship.BatchDisconnectResourcesCmd;
import com.stratocloud.tag.NestedTag;
import com.stratocloud.tag.TagRecord;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class BatchDisconnectResourcesJobHandler implements AsyncJobHandler<BatchDisconnectResourcesCmd> {

    private final ResourceService resourceService;

    private final RelationshipRepository relationshipRepository;

    private final ResourceJobHelper resourceJobHelper;

    public BatchDisconnectResourcesJobHandler(ResourceService resourceService,
                                              RelationshipRepository relationshipRepository,
                                              ResourceJobHelper resourceJobHelper) {
        this.resourceService = resourceService;
        this.relationshipRepository = relationshipRepository;
        this.resourceJobHelper = resourceJobHelper;
    }

    @Override
    public String getJobType() {
        return "BATCH_DISCONNECT_RESOURCES";
    }

    @Override
    public String getJobTypeName() {
        return "解除挂载";
    }

    @Override
    public String getStartJobTopic() {
        return "BATCH_DISCONNECT_RESOURCES_JOB_START";
    }

    @Override
    public String getCancelJobTopic() {
        return "BATCH_DISCONNECT_RESOURCES_JOB_CANCEL";
    }

    @Override
    public String getServiceName() {
        return StratoServices.RESOURCE_SERVICE;
    }

    @Override
    public void preCreateJob(BatchDisconnectResourcesCmd parameters) {
        resourceService.disconnectResources(parameters);
    }

    @Override
    public void onUpdateJob(BatchDisconnectResourcesCmd parameters) {
        AsyncJob asyncJob = AsyncJobContext.current().getAsyncJob();

        asyncJob.discardCurrentExecutions();

        resourceService.disconnectResources(parameters);
    }

    @Override
    public void onCancelJob(String message, BatchDisconnectResourcesCmd parameters) {
        AsyncJob asyncJob = AsyncJobContext.current().getAsyncJob();
        asyncJob.cancel(message);
    }

    @Override
    public void onStartJob(BatchDisconnectResourcesCmd parameters) {
        AsyncJob asyncJob = AsyncJobContext.current().getAsyncJob();
        asyncJob.start();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> prepareRuntimeProperties(BatchDisconnectResourcesCmd jobParameters) {
        List<NestedTag> nestedTags = new ArrayList<>();
        List<Long> resourceIds = new ArrayList<>();

        if(Utils.isNotEmpty(jobParameters.getRelationshipIds())){
            for (Long relationshipId : jobParameters.getRelationshipIds()) {
                Relationship relationship = relationshipRepository.findRelationship(relationshipId);
                resourceIds.add(relationship.getSource().getId());
                if(Utils.isNotEmpty(relationship.getSource().getTags()))
                    nestedTags.addAll(relationship.getSource().getTags());
            }
        }

        return Map.of(
                JobContext.KEY_RELATED_TAGS,
                TagRecord.fromNestedTags(nestedTags),
                JobContext.KEY_RESOURCES,
                resourceJobHelper.getNestedResources(resourceIds)
        );
    }
}
