package com.stratocloud.resource.jobs;

import com.stratocloud.auth.CallContext;
import com.stratocloud.constant.StratoServices;
import com.stratocloud.job.AutoRegisteredJobHandler;
import com.stratocloud.job.JobContext;
import com.stratocloud.messaging.MessageBus;
import com.stratocloud.permission.DynamicPermissionRequired;
import com.stratocloud.permission.PermissionItem;
import com.stratocloud.repository.ResourceRepository;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourcePermissionTarget;
import com.stratocloud.resource.ResourceService;
import com.stratocloud.resource.cmd.BatchClaimCmd;
import com.stratocloud.resource.cmd.BatchTransferCmd;
import com.stratocloud.resource.cmd.ownership.ClaimCmd;
import com.stratocloud.resource.cmd.ownership.TransferCmd;
import org.apache.commons.collections.MapUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class BatchClaimResourcesJobHandler
        implements AutoRegisteredJobHandler<BatchClaimCmd>, DynamicPermissionRequired {

    private final MessageBus messageBus;

    private final ResourceService resourceService;

    private final ResourceRepository resourceRepository;

    public BatchClaimResourcesJobHandler(MessageBus messageBus,
                                         ResourceService resourceService,
                                         ResourceRepository resourceRepository) {
        this.messageBus = messageBus;
        this.resourceService = resourceService;
        this.resourceRepository = resourceRepository;
    }


    @Override
    public String getJobType() {
        return "CLAIM_RESOURCES";
    }

    @Override
    public String getJobTypeName() {
        return "认领资源";
    }

    @Override
    public String getStartJobTopic() {
        return "CLAIM_RESOURCES_JOB_START";
    }

    @Override
    public String getCancelJobTopic() {
        return "CLAIM_RESOURCES_JOB_CANCEL";
    }

    @Override
    public String getServiceName() {
        return StratoServices.RESOURCE_SERVICE;
    }

    @Override
    public void preCreateJob(BatchClaimCmd parameters) {
        JobContext.current().addOutput("newOwnerId", CallContext.current().getCallingUser().userId());
        JobContext.current().addOutput("newTenantId", CallContext.current().getCallingUser().tenantId());
    }

    @Override
    public void onUpdateJob(BatchClaimCmd parameters) {

    }

    @Override
    public void onCancelJob(String message, BatchClaimCmd parameters) {

    }

    @Override
    public void onStartJob(BatchClaimCmd parameters) {
        BatchTransferCmd cmd = new BatchTransferCmd();

        Long newOwnerId = MapUtils.getLong(JobContext.current().getRuntimeVariables(), "newOwnerId");
        Long newTenantId = MapUtils.getLong(JobContext.current().getRuntimeVariables(), "newTenantId");

        List<TransferCmd> transfers = new ArrayList<>();
        for (ClaimCmd claim : parameters.getClaims()) {
            TransferCmd transfer = new TransferCmd();
            transfer.setResourceId(claim.getResourceId());
            transfer.setNewOwnerId(newOwnerId);
            transfer.setNewTenantId(newTenantId);
            transfer.setEnableCascadedTransfer(claim.getEnableCascadedClaim());
            transfers.add(transfer);
        }

        cmd.setTransfers(transfers);

        CallContext currentContext = CallContext.current();
        CallContext.registerSystemSession();
        tryFinishJob(messageBus, ()->resourceService.transfer(cmd));
        CallContext.registerBack(currentContext);
    }

    @Override
    public List<String> collectSummaryData(BatchClaimCmd jobParameters) {
        String userName = CallContext.current().getCallingUser().realName();

        List<Long> resourceIds = jobParameters.getClaims().stream().map(
                ClaimCmd::getResourceId
        ).toList();

        List<Resource> resources = resourceRepository.findAllById(resourceIds);

        List<String> resourceNames = resources.stream().map(Resource::getName).toList();

        return List.of("%s认领资源: %s".formatted(userName, String.join(",", resourceNames)));
    }

    @Override
    public PermissionItem getPermissionItem() {
        return new PermissionItem(
                ResourcePermissionTarget.ID,
                ResourcePermissionTarget.NAME,
                "CLAIM",
                "认领"
        );
    }
}
