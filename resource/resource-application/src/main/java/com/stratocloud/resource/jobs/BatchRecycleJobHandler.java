package com.stratocloud.resource.jobs;

import com.stratocloud.constant.StratoServices;
import com.stratocloud.job.AsyncJob;
import com.stratocloud.job.AsyncJobContext;
import com.stratocloud.job.AsyncJobHandler;
import com.stratocloud.job.JobContext;
import com.stratocloud.permission.DynamicPermissionRequired;
import com.stratocloud.permission.PermissionItem;
import com.stratocloud.repository.ResourceRepository;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourcePermissionTarget;
import com.stratocloud.resource.ResourceService;
import com.stratocloud.resource.cmd.BatchRecycleCmd;
import com.stratocloud.resource.cmd.recycle.RecycleCmd;
import com.stratocloud.tag.NestedTag;
import com.stratocloud.tag.TagRecord;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class BatchRecycleJobHandler implements AsyncJobHandler<BatchRecycleCmd>, DynamicPermissionRequired {

    private final ResourceService resourceService;

    private final ResourceRepository resourceRepository;

    public BatchRecycleJobHandler(ResourceService resourceService,
                                  ResourceRepository resourceRepository) {
        this.resourceService = resourceService;
        this.resourceRepository = resourceRepository;
    }

    @Override
    public String getJobType() {
        return "BATCH_RECYCLE_RESOURCES";
    }

    @Override
    public String getJobTypeName() {
        return "回收资源";
    }

    @Override
    public String getStartJobTopic() {
        return "BATCH_RECYCLE_RESOURCES_JOB_START";
    }

    @Override
    public String getCancelJobTopic() {
        return "BATCH_RECYCLE_RESOURCES_JOB_CANCEL";
    }

    @Override
    public String getServiceName() {
        return StratoServices.RESOURCE_SERVICE;
    }

    @Override
    public void preCreateJob(BatchRecycleCmd parameters) {
        resourceService.recycle(parameters);
    }

    @Override
    public void onUpdateJob(BatchRecycleCmd parameters) {
        AsyncJob asyncJob = AsyncJobContext.current().getAsyncJob();

        asyncJob.discardCurrentExecutions();

        resourceService.recycle(parameters);
    }

    @Override
    public void onCancelJob(String message, BatchRecycleCmd parameters) {
        AsyncJob asyncJob = AsyncJobContext.current().getAsyncJob();
        asyncJob.cancel(message);
    }

    @Override
    public void onStartJob(BatchRecycleCmd parameters) {
        AsyncJob asyncJob = AsyncJobContext.current().getAsyncJob();
        asyncJob.start();
    }

    @Override
    public PermissionItem getPermissionItem() {
        return new PermissionItem(
                ResourcePermissionTarget.ID, ResourcePermissionTarget.NAME, getJobType(), getJobTypeName()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> prepareRuntimeProperties(BatchRecycleCmd jobParameters) {
        List<NestedTag> nestedTags = new ArrayList<>();

        if(Utils.isNotEmpty(jobParameters.getResources())){
            for (RecycleCmd recycleCmd : jobParameters.getResources()) {
                Resource resource = resourceRepository.findResource(recycleCmd.getResourceId());

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
