package com.stratocloud.provider.tencent.disk.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.ExternalResourceNotFoundException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.constants.UsageTypes;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.common.TencentTimeUtil;
import com.stratocloud.provider.tencent.disk.TencentDiskHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.concurrent.SleepUtil;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.cbs.v20170312.models.Disk;
import com.tencentcloudapi.cbs.v20170312.models.InquiryPriceResizeDiskRequest;
import com.tencentcloudapi.cbs.v20170312.models.PrepayPrice;
import com.tencentcloudapi.cbs.v20170312.models.ResizeDiskRequest;
import com.tencentcloudapi.cvm.v20170312.models.ResizeInstanceDisksRequest;
import com.tencentcloudapi.cvm.v20170312.models.SystemDisk;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class TencentDiskResizeHandler implements ResourceActionHandler {

    private final TencentDiskHandler diskHandler;

    public TencentDiskResizeHandler(TencentDiskHandler diskHandler) {
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
        return TencentDiskResizeInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        TencentDiskResizeInput resizeInput = JSON.convert(parameters, TencentDiskResizeInput.class);
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) diskHandler.getProvider();

        Disk disk = diskHandler.describeDisk(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Disk not found.")
        );

        if(disk.getPortable()){
            ResizeDiskRequest request = new ResizeDiskRequest();
            request.setDiskId(resource.getExternalId());
            request.setDiskSize(resizeInput.getDiskSize());

            provider.buildClient(account).resizeDisk(request);
        }else {
            SystemDisk systemDisk = new SystemDisk();
            systemDisk.setDiskSize(resizeInput.getDiskSize());

            ResizeInstanceDisksRequest request = new ResizeInstanceDisksRequest();
            request.setInstanceId(disk.getInstanceId());
            request.setSystemDisk(systemDisk);
            request.setResizeOnline(true);

            provider.buildClient(account).resizeInstanceDisks(request);
        }



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
        TencentDiskResizeInput resizeInput = JSON.convert(parameters, TencentDiskResizeInput.class);
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Optional<Disk> disk = diskHandler.describeDisk(account, resource.getExternalId());

        if(disk.isEmpty())
            return List.of();

        if(resizeInput.getDiskSize() <= disk.get().getDiskSize())
            return List.of();

        long sizeChange = resizeInput.getDiskSize() - disk.get().getDiskSize();

        return List.of(new ResourceUsage(
                UsageTypes.DISK_GB.type(), BigDecimal.valueOf(sizeChange)
        ));
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        TencentDiskResizeInput resizeInput = JSON.convert(parameters, TencentDiskResizeInput.class);
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        Disk disk = diskHandler.describeDisk(account, resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Disk not found.")
        );

        if(resizeInput.getDiskSize() <= disk.getDiskSize())
            throw new StratoException("扩容大小必须大于当前大小");
    }


    @Override
    public ResourceCost getActionCost(Resource resource, Map<String, Object> parameters) {
        if(Utils.isBlank(resource.getExternalId()))
            return ResourceCost.ZERO;

        TencentDiskResizeInput resizeInput = JSON.convert(parameters, TencentDiskResizeInput.class);

        TencentCloudProvider provider = (TencentCloudProvider) diskHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudClient client = provider.buildClient(account);

        Disk disk = client.describeDisk(resource.getExternalId()).orElseThrow(
                () -> new ExternalResourceNotFoundException("Disk not found.")
        );

        if(resizeInput.getDiskSize()==null || resizeInput.getDiskSize() <= disk.getDiskSize())
            return ResourceCost.ZERO;

        if(!disk.getPortable()){
            return ResourceCost.ZERO;
        }

        InquiryPriceResizeDiskRequest inquiry = new InquiryPriceResizeDiskRequest();

        inquiry.setDiskId(disk.getDiskId());
        inquiry.setDiskSize(resizeInput.getDiskSize());

        PrepayPrice price = client.inquiryPriceResizeDisk(inquiry);

        if(Objects.equals(disk.getDiskChargeType(), "PREPAID")) {
            LocalDateTime deadline = TencentTimeUtil.toLocalDateTime(disk.getDeadlineTime());
            long months = ChronoUnit.MONTHS.between(LocalDateTime.now(), deadline);
            return new ResourceCost(price.getDiscountPrice(), months, ChronoUnit.MONTHS);
        }else {
            return new ResourceCost(price.getUnitPriceDiscount(), 1.0, ChronoUnit.HOURS);
        }
    }
}
