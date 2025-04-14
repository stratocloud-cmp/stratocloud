package com.stratocloud.resource.jobs;

import com.stratocloud.constant.StratoServices;
import com.stratocloud.job.AsyncJob;
import com.stratocloud.job.AsyncJobContext;
import com.stratocloud.job.AsyncJobHandler;
import com.stratocloud.job.JobContext;
import com.stratocloud.repository.ResourceRepository;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceService;
import com.stratocloud.resource.cmd.relationship.BatchConnectResourcesCmd;
import com.stratocloud.resource.cmd.relationship.ConnectResourcesCmd;
import com.stratocloud.tag.NestedTag;
import com.stratocloud.tag.TagRecord;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class BatchConnectResourcesJobHandler implements AsyncJobHandler<BatchConnectResourcesCmd> {

    private final ResourceService resourceService;

    private final ResourceRepository resourceRepository;

    public BatchConnectResourcesJobHandler(ResourceService resourceService,
                                           ResourceRepository resourceRepository) {
        this.resourceService = resourceService;
        this.resourceRepository = resourceRepository;
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

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> prepareRuntimeProperties(BatchConnectResourcesCmd jobParameters) {
        List<NestedTag> nestedTags = new ArrayList<>();

        if(Utils.isNotEmpty(jobParameters.getConnections())){
            for (ConnectResourcesCmd connection : jobParameters.getConnections()) {
                Resource resource = resourceRepository.findResource(connection.getSourceResourceId());

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
