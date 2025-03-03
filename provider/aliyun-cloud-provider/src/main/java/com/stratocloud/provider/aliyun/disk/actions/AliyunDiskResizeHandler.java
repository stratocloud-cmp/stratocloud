package com.stratocloud.provider.aliyun.disk.actions;

import com.aliyun.ecs20140526.models.DescribePriceRequest;
import com.aliyun.ecs20140526.models.ResizeDiskRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.aliyun.disk.AliyunDisk;
import com.stratocloud.provider.aliyun.disk.AliyunDiskHandler;
import com.stratocloud.provider.constants.UsageTypes;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.concurrent.SleepUtil;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class AliyunDiskResizeHandler implements ResourceActionHandler {

    private final AliyunDiskHandler diskHandler;

    public AliyunDiskResizeHandler(AliyunDiskHandler diskHandler) {
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
        return "变更云硬盘大小";
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
        return AliyunDiskResizeInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        AliyunDiskResizeInput resizeInput = JSON.convert(parameters, AliyunDiskResizeInput.class);
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) diskHandler.getProvider();

        ResizeDiskRequest request = new ResizeDiskRequest();
        request.setDiskId(resource.getExternalId());
        request.setNewSize(resizeInput.getDiskSize());

        provider.buildClient(account).ecs().resizeDisk(request);
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Optional<ExternalResource> disk = diskHandler.describeExternalResource(account, resource.getExternalId());

        if(disk.isEmpty())
            return ResourceActionResult.failed("Disk not found.");

        if(disk.get().state() == ResourceState.CONFIGURING)
            return ResourceActionResult.inProgress();

        SleepUtil.sleep(10);

        return ResourceActionResult.finished();
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        AliyunDiskResizeInput resizeInput = JSON.convert(parameters, AliyunDiskResizeInput.class);
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<AliyunDisk> disk = diskHandler.describeDisk(account, resource.getExternalId());

        if(disk.isEmpty())
            return List.of();

        if(resizeInput.getDiskSize() <= disk.get().detail().getSize())
            return List.of();

        long sizeChange = resizeInput.getDiskSize() - disk.get().detail().getSize();

        return List.of(new ResourceUsage(
                UsageTypes.DISK_GB.type(), BigDecimal.valueOf(sizeChange)
        ));
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        AliyunDiskResizeInput resizeInput = JSON.convert(parameters, AliyunDiskResizeInput.class);
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunDisk disk = diskHandler.describeDisk(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Disk not found.")
        );

        if(resizeInput.getDiskSize() <= disk.detail().getSize())
            throw new StratoException("扩容大小必须大于当前大小");
    }


    @Override
    public ResourceCost getActionCost(Resource resource, Map<String, Object> parameters) {
        if(Utils.isBlank(resource.getExternalId()))
            return ResourceCost.ZERO;

        AliyunDiskResizeInput resizeInput = JSON.convert(parameters, AliyunDiskResizeInput.class);

        AliyunCloudProvider provider = (AliyunCloudProvider) diskHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunClient client = provider.buildClient(account);

        AliyunDisk disk = client.ecs().describeDisk(resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Disk not found.")
        );

        if(resizeInput.getDiskSize()==null || resizeInput.getDiskSize() <= disk.detail().getSize())
            return ResourceCost.ZERO;

        var request = new DescribePriceRequest();

        request.setResourceType("disk");

        var dataDisk = new DescribePriceRequest.DescribePriceRequestDataDisk();

        dataDisk.setCategory(disk.detail().getCategory());
        dataDisk.setSize(Long.valueOf(resizeInput.getDiskSize()));
        dataDisk.setPerformanceLevel(disk.detail().getPerformanceLevel());

        request.setDataDisk(List.of(dataDisk));

        var priceInfo = provider.buildClient(account).ecs().describePrice(request).getPriceInfo();

        return new ResourceCost(
                priceInfo.getPrice().getTradePrice(),
                1.0,
                ChronoUnit.HOURS
        );
    }
}
