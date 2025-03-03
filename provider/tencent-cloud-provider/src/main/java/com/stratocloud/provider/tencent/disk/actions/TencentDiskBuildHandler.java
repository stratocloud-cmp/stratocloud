package com.stratocloud.provider.tencent.disk.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.constants.UsageTypes;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.disk.TencentDiskHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceCost;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import com.tencentcloudapi.cbs.v20170312.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
public class TencentDiskBuildHandler implements BuildResourceActionHandler {
    private final TencentDiskHandler diskHandler;

    public TencentDiskBuildHandler(TencentDiskHandler diskHandler) {
        this.diskHandler = diskHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return diskHandler;
    }

    @Override
    public String getTaskName() {
        return "创建云硬盘";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return TencentDiskBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        TencentDiskBuildInput input = JSON.convert(parameters, TencentDiskBuildInput.class);

        if(resource.isPrimaryTo(ResourceCategories.COMPUTE_INSTANCE))
            configureSystemDisk(resource, input);
        else
            createDataDisk(resource, input);
    }

    private void configureSystemDisk(Resource resource, TencentDiskBuildInput input) {
        log.info("Configuring Tencent system disk: {}.", resource.getName());

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) diskHandler.getProvider();
        TencentCloudClient client = provider.buildClient(account);

        Optional<Disk> disk = client.describeDisk(resource.getExternalId());

        if(disk.isEmpty()){
            log.warn("Tencent system disk {} not created yet, something went wrong.", resource.getName());
            return;
        }

        ModifyDiskAttributesRequest modifyRequest = new ModifyDiskAttributesRequest();

        modifyRequest.setDiskIds(new String[]{disk.get().getDiskId()});
        modifyRequest.setDiskName(resource.getName());

        boolean enableAdvanceOptions = input.getEnableAdvanceOptions() != null && input.getEnableAdvanceOptions();
        boolean burstPerformance = input.getBurstPerformance() != null && input.getBurstPerformance();

        if(enableAdvanceOptions && burstPerformance)
            modifyRequest.setBurstPerformanceOperation("CREATE");

        log.info("Modifying Tencent system disk attributes...");
        client.modifyDisk(modifyRequest);

        if(enableAdvanceOptions){
            if(input.getBackupQuota() != null){
                ModifyDiskBackupQuotaRequest modifyBackupQuotaRequest = new ModifyDiskBackupQuotaRequest();
                modifyBackupQuotaRequest.setDiskId(disk.get().getDiskId());
                modifyBackupQuotaRequest.setDiskBackupQuota(input.getBackupQuota());

                log.info("Modifying Tencent system disk backup quota...");
                client.modifyDiskBackupQuota(modifyBackupQuotaRequest);
            }

            if(input.getThroughputPerformance() != null){
                var modifyDiskExtraPerformanceRequest = new ModifyDiskExtraPerformanceRequest();
                modifyDiskExtraPerformanceRequest.setDiskId(disk.get().getDiskId());
                modifyDiskExtraPerformanceRequest.setThroughputPerformance(input.getThroughputPerformance());

                log.info("Modifying Tencent system disk extra performance...");
                client.modifyDiskExtraPerformance(modifyDiskExtraPerformanceRequest);
            }
        }

        log.info("Tencent system disk {} configured successfully.", resource.getName());
    }

    private void createDataDisk(Resource resource, TencentDiskBuildInput input) {
        log.info("Creating Tencent data disk: {}.", resource.getName());

        CreateDisksRequest request = toCreateDiskRequest(resource, input);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) diskHandler.getProvider();

        String diskId = provider.buildClient(account).createDisk(request);
        resource.setExternalId(diskId);

        log.info("Tencent data disk {} created successfully. External ID: {}.", resource.getName(), diskId);
    }

    private CreateDisksRequest toCreateDiskRequest(Resource resource, TencentDiskBuildInput input) {
        CreateDisksRequest request = new CreateDisksRequest();

        resolveBasicOptions(resource, input, request);

        resolvePlacement(resource, request);

        resolveChargeOptions(input, request);

        resolveAdvanceOptions(input, request);

        return request;
    }

    private void resolveAdvanceOptions(TencentDiskBuildInput input, CreateDisksRequest request) {
        if(input.getEnableAdvanceOptions() == null || !input.getEnableAdvanceOptions())
            return;

        if(input.getDiskType() == TencentDiskType.CLOUD_TSSD || input.getDiskType() == TencentDiskType.CLOUD_HSSD)
            request.setThroughputPerformance(input.getThroughputPerformance());

        boolean encrypted = input.getEncrypt() != null && input.getEncrypt();
        request.setEncrypt(encrypted?"ENCRYPT":null);

        request.setDiskBackupQuota(input.getBackupQuota());

        request.setBurstPerformance(input.getBurstPerformance());
    }

    private void resolveChargeOptions(TencentDiskBuildInput input, CreateDisksRequest request) {
        request.setDiskChargeType(input.getChargeType());
        if(Objects.equals("PREPAID", input.getChargeType())){
            DiskChargePrepaid prepaid = new DiskChargePrepaid();
            prepaid.setPeriod(Long.valueOf(input.getPrepaidPeriod()));
            prepaid.setRenewFlag(input.getRenewType());
            request.setDiskChargePrepaid(prepaid);
        }
    }

    private void resolvePlacement(Resource resource, CreateDisksRequest request) {
        Resource zone = resource.getEssentialTarget(ResourceCategories.ZONE).orElseThrow(
                () -> new StratoException("No zone found when creating disk.")
        );

        Placement placement = new Placement();
        placement.setZone(zone.getExternalId());

        request.setPlacement(placement);
    }

    private void resolveBasicOptions(Resource resource, TencentDiskBuildInput input, CreateDisksRequest request) {
        request.setDiskName(resource.getName());
        request.setDiskSize(input.getDiskSize());

        if(input.getDiskType() != null)
            request.setDiskType(input.getDiskType().getId());
    }


    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        TencentDiskBuildInput input = JSON.convert(parameters, TencentDiskBuildInput.class);

        return List.of(new ResourceUsage(
                UsageTypes.DISK_GB.type(),
                BigDecimal.valueOf(input.getDiskSize())
        ));
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }

    @Override
    public ResourceCost getActionCost(Resource resource, Map<String, Object> parameters) {
        if(resource.isPrimaryTo(ResourceCategories.COMPUTE_INSTANCE))
            return ResourceCost.ZERO;

        TencentDiskBuildInput input = JSON.convert(parameters, TencentDiskBuildInput.class);
        CreateDisksRequest request = toCreateDiskRequest(resource, input);

        InquiryPriceCreateDisksRequest inquiry = new InquiryPriceCreateDisksRequest();

        inquiry.setDiskChargeType(request.getDiskChargeType());
        inquiry.setDiskType(request.getDiskType());
        inquiry.setDiskSize(request.getDiskSize());
        inquiry.setThroughputPerformance(request.getThroughputPerformance());
        inquiry.setDiskChargePrepaid(request.getDiskChargePrepaid());
        inquiry.setDiskBackupQuota(request.getDiskBackupQuota());

        TencentCloudProvider provider = (TencentCloudProvider) diskHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Price price = provider.buildClient(account).inquiryPriceCreateDisk(inquiry);

        if(Objects.equals(request.getDiskChargeType(), "PREPAID")) {
            long period = 1L;
            DiskChargePrepaid prepaid = request.getDiskChargePrepaid();
            if(prepaid != null && prepaid.getPeriod() != null)
                period = prepaid.getPeriod();
            return new ResourceCost(price.getDiscountPrice(), period, ChronoUnit.MONTHS);
        }else {
            return new ResourceCost(price.getUnitPriceDiscount(), 1.0, ChronoUnit.HOURS);
        }
    }
}
