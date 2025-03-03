package com.stratocloud.provider.huawei.disk.actions;

import com.huaweicloud.sdk.evs.v2.model.UpdateVolumeOption;
import com.huaweicloud.sdk.evs.v2.model.UpdateVolumeRequest;
import com.huaweicloud.sdk.evs.v2.model.UpdateVolumeRequestBody;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.disk.HuaweiDiskHandler;
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
public class HuaweiDiskUpdateHandler implements ResourceActionHandler {

    private final HuaweiDiskHandler diskHandler;

    public HuaweiDiskUpdateHandler(HuaweiDiskHandler diskHandler) {
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
        return Optional.of(
                ResourceState.CONFIGURING
        );
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return HuaweiDiskUpdateInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        HuaweiDiskUpdateInput input = JSON.convert(parameters, HuaweiDiskUpdateInput.class);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        HuaweiCloudProvider provider = (HuaweiCloudProvider) diskHandler.getProvider();

        provider.buildClient(account).evs().updateVolume(
                new UpdateVolumeRequest().withVolumeId(resource.getExternalId()).withBody(
                        new UpdateVolumeRequestBody().withVolume(
                                new UpdateVolumeOption().withName(input.getDiskName())
                        )
                )
        );
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
