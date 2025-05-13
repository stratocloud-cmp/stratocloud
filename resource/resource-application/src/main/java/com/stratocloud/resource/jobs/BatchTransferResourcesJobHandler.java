package com.stratocloud.resource.jobs;

import com.stratocloud.constant.StratoServices;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.external.resource.UserGatewayService;
import com.stratocloud.identity.SimpleUser;
import com.stratocloud.job.AutoRegisteredJobHandler;
import com.stratocloud.job.JobContext;
import com.stratocloud.messaging.MessageBus;
import com.stratocloud.permission.DynamicPermissionRequired;
import com.stratocloud.permission.PermissionItem;
import com.stratocloud.repository.ResourceRepository;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceJobHelper;
import com.stratocloud.resource.ResourcePermissionTarget;
import com.stratocloud.resource.ResourceService;
import com.stratocloud.resource.cmd.BatchTransferCmd;
import com.stratocloud.resource.cmd.ownership.TransferCmd;
import com.stratocloud.tag.NestedTag;
import com.stratocloud.tag.TagRecord;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class BatchTransferResourcesJobHandler
        implements AutoRegisteredJobHandler<BatchTransferCmd>, DynamicPermissionRequired {

    private final MessageBus messageBus;

    private final ResourceService resourceService;

    private final ResourceRepository resourceRepository;

    private final UserGatewayService userGatewayService;

    private final ResourceJobHelper resourceJobHelper;

    public BatchTransferResourcesJobHandler(MessageBus messageBus,
                                            ResourceService resourceService,
                                            ResourceRepository resourceRepository,
                                            UserGatewayService userGatewayService,
                                            ResourceJobHelper resourceJobHelper) {
        this.messageBus = messageBus;
        this.resourceService = resourceService;
        this.resourceRepository = resourceRepository;
        this.userGatewayService = userGatewayService;
        this.resourceJobHelper = resourceJobHelper;
    }


    @Override
    public String getJobType() {
        return "TRANSFER_RESOURCES";
    }

    @Override
    public String getJobTypeName() {
        return "移交资源";
    }

    @Override
    public String getStartJobTopic() {
        return "TRANSFER_RESOURCES_JOB_START";
    }

    @Override
    public String getCancelJobTopic() {
        return "TRANSFER_RESOURCES_JOB_CANCEL";
    }

    @Override
    public String getServiceName() {
        return StratoServices.RESOURCE_SERVICE;
    }

    @Override
    public void preCreateJob(BatchTransferCmd parameters) {

    }

    @Override
    public void onUpdateJob(BatchTransferCmd parameters) {

    }

    @Override
    public void onCancelJob(String message, BatchTransferCmd parameters) {

    }

    @Override
    public void onStartJob(BatchTransferCmd parameters) {
        tryFinishJob(messageBus, ()->resourceService.transfer(parameters));
    }

    @Override
    public List<String> collectSummaryData(BatchTransferCmd jobParameters) {
        List<String> result = new ArrayList<>();

        for (TransferCmd transfer : jobParameters.getTransfers()) {
            Resource resource = resourceRepository.findResource(transfer.getResourceId());

            SimpleUser simpleUser = userGatewayService.findUser(transfer.getNewOwnerId()).orElseThrow(
                    () -> new StratoException("Transfer target user not found.")
            );

            result.add("资源[%s]移交至%s".formatted(resource.getName(), simpleUser.realName()));
        }

        return result;
    }

    @Override
    public PermissionItem getPermissionItem() {
        return new PermissionItem(
                ResourcePermissionTarget.ID,
                ResourcePermissionTarget.NAME,
                "TRANSFER",
                "移交"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> prepareRuntimeProperties(BatchTransferCmd jobParameters) {
        List<NestedTag> nestedTags = new ArrayList<>();
        List<Long> resourceIds = new ArrayList<>();

        if(Utils.isNotEmpty(jobParameters.getTransfers())){
            for (TransferCmd transferCmd : jobParameters.getTransfers()) {
                Resource resource = resourceRepository.findResource(transferCmd.getResourceId());
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
