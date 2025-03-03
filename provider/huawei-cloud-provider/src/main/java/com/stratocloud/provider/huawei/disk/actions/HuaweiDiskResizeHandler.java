package com.stratocloud.provider.huawei.disk.actions;

import com.huaweicloud.sdk.evs.v2.model.VolumeDetail;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.exceptions.InvalidArgumentException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.constants.UsageTypes;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.disk.HuaweiDiskHandler;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class HuaweiDiskResizeHandler implements ResourceActionHandler {

    private final HuaweiDiskHandler diskHandler;

    public HuaweiDiskResizeHandler(HuaweiDiskHandler diskHandler) {
        this.diskHandler = diskHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return diskHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.RESIZE;
    }

    @Override
    public String getTaskName() {
        return "云硬盘扩容";
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
        return HuaweiDiskResizeInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) diskHandler.getProvider();
        HuaweiDiskResizeInput input = JSON.convert(parameters, HuaweiDiskResizeInput.class);

        VolumeDetail volumeDetail = diskHandler.describeDisk(account, resource.getExternalId()).orElseThrow(
                () -> new StratoException("Volume not found")
        );

        provider.buildClient(account).evs().resizeVolume(volumeDetail.getId(), input.getDiskSize());
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        return ResourceActionResult.finished();
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<VolumeDetail> volumeDetail = diskHandler.describeDisk(account, resource.getExternalId());

        if(volumeDetail.isEmpty())
            return List.of();

        Integer currentSize = volumeDetail.get().getSize();

        Integer newSize = JSON.convert(parameters, HuaweiDiskResizeInput.class).getDiskSize();

        if(currentSize > newSize)
            return List.of();

        return List.of(
                new ResourceUsage(
                        UsageTypes.DISK_GB.type(), BigDecimal.valueOf(newSize - currentSize)
                )
        );
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        VolumeDetail volumeDetail = diskHandler.describeDisk(account, resource.getExternalId()).orElseThrow(
                () -> new StratoException("Volume not found")
        );

        Integer currentSize = volumeDetail.getSize();

        Integer newSize = JSON.convert(parameters, HuaweiDiskResizeInput.class).getDiskSize();

        if(newSize == null)
            throw new InvalidArgumentException("新磁盘大小不能为空");

        if(currentSize > newSize)
            throw new BadCommandException("新磁盘大小不能小于现有大小");
    }
}
