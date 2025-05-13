package com.stratocloud.resource.jobs;

import com.stratocloud.constant.StratoServices;
import com.stratocloud.job.AsyncJob;
import com.stratocloud.job.AsyncJobContext;
import com.stratocloud.job.AsyncJobHandler;
import com.stratocloud.job.JobContext;
import com.stratocloud.resource.ResourceJobHelper;
import com.stratocloud.resource.ResourceService;
import com.stratocloud.resource.cmd.BatchCreateResourcesCmd;
import com.stratocloud.resource.cmd.create.CreateResourcesCmd;
import com.stratocloud.tag.NestedTag;
import com.stratocloud.tag.TagRecord;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class BatchCreateResourcesJobHandler implements AsyncJobHandler<BatchCreateResourcesCmd> {

    private final ResourceService resourceService;

    private final ResourceJobHelper resourceJobHelper;

    public BatchCreateResourcesJobHandler(ResourceService resourceService,
                                          ResourceJobHelper resourceJobHelper) {
        this.resourceService = resourceService;
        this.resourceJobHelper = resourceJobHelper;
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
        List<Long> resourceIds = resourceService.create(parameters).getResourceIds();

        resourceJobHelper.addResourcesToJobContext(resourceIds);
    }

    @Override
    public void onUpdateJob(BatchCreateResourcesCmd parameters) {
        AsyncJob asyncJob = AsyncJobContext.current().getAsyncJob();

        asyncJob.discardCurrentExecutions();

        List<Long> resourceIds = resourceService.create(parameters).getResourceIds();

        resourceJobHelper.addResourcesToJobContext(resourceIds);
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

    @Override
    public Map<String, Object> prepareRuntimeProperties(BatchCreateResourcesCmd jobParameters) {
        List<NestedTag> nestedTags = new ArrayList<>();

        if(Utils.isNotEmpty(jobParameters.getResources())){
            for (CreateResourcesCmd resource : jobParameters.getResources()) {
                if(resource.getResource()!=null && Utils.isNotEmpty(resource.getResource().getTags())){
                    nestedTags.addAll(resource.getResource().getTags());
                }
            }
        }

        return Map.of(
                JobContext.KEY_RELATED_TAGS,
                TagRecord.fromNestedTags(nestedTags)
        );
    }
}
