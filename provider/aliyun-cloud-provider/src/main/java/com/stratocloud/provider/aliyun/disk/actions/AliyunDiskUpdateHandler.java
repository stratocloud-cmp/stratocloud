package com.stratocloud.provider.aliyun.disk.actions;

import com.aliyun.ecs20140526.models.ModifyDiskAttributeRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.disk.AliyunDiskHandler;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class AliyunDiskUpdateHandler implements ResourceActionHandler {

    private final AliyunDiskHandler diskHandler;

    public AliyunDiskUpdateHandler(AliyunDiskHandler diskHandler) {
        this.diskHandler = diskHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return diskHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.UPDATE;
    }

    @Override
    public String getTaskName() {
        return "更新云硬盘";
    }

    @Override
    public Set<ResourceState> getAllowedStates() {
        return ResourceState.getAliveStateSet();
    }

    @Override
    public Optional<ResourceState> getTransitionState() {
        return Optional.of(ResourceState.CONFIGURING);
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return AliyunDiskUpdateInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        AliyunDiskUpdateInput updateInput = JSON.convert(parameters, AliyunDiskUpdateInput.class);
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) diskHandler.getProvider();

        ModifyDiskAttributeRequest request = new ModifyDiskAttributeRequest();

        request.setDiskId(resource.getExternalId());
        request.setDiskName(updateInput.getDiskName());

        request.setBurstingEnabled(updateInput.getBurstPerformance());

        provider.buildClient(account).ecs().modifyDisk(request);
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        return ResourceActionResult.finished();
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }
}
