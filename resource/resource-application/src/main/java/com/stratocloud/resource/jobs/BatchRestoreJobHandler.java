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
import com.stratocloud.resource.ResourceJobHelper;
import com.stratocloud.resource.ResourcePermissionTarget;
import com.stratocloud.resource.ResourceService;
import com.stratocloud.resource.cmd.BatchRestoreCmd;
import com.stratocloud.tag.NestedTag;
import com.stratocloud.tag.TagRecord;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class BatchRestoreJobHandler implements AsyncJobHandler<BatchRestoreCmd>, DynamicPermissionRequired {

    private final ResourceService resourceService;

    private final ResourceRepository resourceRepository;

    private final ResourceJobHelper resourceJobHelper;

    public BatchRestoreJobHandler(ResourceService resourceService,
                                  ResourceRepository resourceRepository,
                                  ResourceJobHelper resourceJobHelper) {
        this.resourceService = resourceService;
        this.resourceRepository = resourceRepository;
        this.resourceJobHelper = resourceJobHelper;
    }

    @Override
    public String getJobType() {
        return "BATCH_RESTORE_RESOURCES";
    }

    @Override
    public String getJobTypeName() {
        return "还原资源";
    }

    @Override
    public String getStartJobTopic() {
        return "BATCH_RESTORE_RESOURCES_JOB_START";
    }

    @Override
    public String getCancelJobTopic() {
        return "BATCH_RESTORE_RESOURCES_JOB_CANCEL";
    }

    @Override
    public String getServiceName() {
        return StratoServices.RESOURCE_SERVICE;
    }

    @Override
    public void preCreateJob(BatchRestoreCmd parameters) {
        resourceService.restore(parameters);
    }

    @Override
    public void onUpdateJob(BatchRestoreCmd parameters) {
        AsyncJob asyncJob = AsyncJobContext.current().getAsyncJob();

        asyncJob.discardCurrentExecutions();

        resourceService.restore(parameters);
    }

    @Override
    public void onCancelJob(String message, BatchRestoreCmd parameters) {
        AsyncJob asyncJob = AsyncJobContext.current().getAsyncJob();
        asyncJob.cancel(message);
    }

    @Override
    public void onStartJob(BatchRestoreCmd parameters) {
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
    public Map<String, Object> prepareRuntimeProperties(BatchRestoreCmd jobParameters) {
        List<NestedTag> nestedTags = new ArrayList<>();
        List<Long> resourceIds = new ArrayList<>();

        if(Utils.isNotEmpty(jobParameters.getResourceIds())){
            for (Long resourceId : jobParameters.getResourceIds()) {
                Resource resource = resourceRepository.findResource(resourceId);
                resourceIds.add(resource.getId());

                if(Utils.isNotEmpty(resource.getTags()))
                    nestedTags.addAll(resource.getTags());
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
