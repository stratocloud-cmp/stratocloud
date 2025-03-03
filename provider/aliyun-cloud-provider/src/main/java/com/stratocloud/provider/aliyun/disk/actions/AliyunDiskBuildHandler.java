package com.stratocloud.provider.aliyun.disk.actions;

import com.aliyun.ecs20140526.models.CreateDiskRequest;
import com.aliyun.ecs20140526.models.DescribePriceRequest;
import com.aliyun.ecs20140526.models.ModifyDiskAttributeRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.aliyun.disk.AliyunDisk;
import com.stratocloud.provider.aliyun.disk.AliyunDiskCategory;
import com.stratocloud.provider.aliyun.disk.AliyunDiskHandler;
import com.stratocloud.provider.aliyun.instance.AliyunInstanceUtil;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.constants.UsageTypes;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceCost;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class AliyunDiskBuildHandler implements BuildResourceActionHandler {
    private final AliyunDiskHandler diskHandler;

    public AliyunDiskBuildHandler(AliyunDiskHandler diskHandler) {
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
        return AliyunDiskBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        AliyunDiskBuildInput input = JSON.convert(parameters, AliyunDiskBuildInput.class);

        if(resource.isPrimaryTo(ResourceCategories.COMPUTE_INSTANCE))
            configureSystemDisk(resource, input);
        else
            createDataDisk(resource, input);
    }

    private void configureSystemDisk(Resource resource, AliyunDiskBuildInput input) {
        log.info("Configuring Aliyun system disk: {}.", resource.getName());

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) diskHandler.getProvider();
        AliyunClient client = provider.buildClient(account);

        Optional<AliyunDisk> disk = client.ecs().describeDisk(resource.getExternalId());

        if(disk.isEmpty()){
            log.warn("Aliyun system disk {} not created yet, something went wrong.", resource.getName());
            return;
        }

        ModifyDiskAttributeRequest modifyRequest = new ModifyDiskAttributeRequest();

        modifyRequest.setDiskId(disk.get().detail().getDiskId());
        modifyRequest.setDiskName(resource.getName());

        boolean enableAdvanceOptions = input.getEnableAdvanceOptions() != null && input.getEnableAdvanceOptions();
        boolean burstPerformance = input.getBurstPerformance() != null && input.getBurstPerformance();

        if(enableAdvanceOptions && burstPerformance)
            modifyRequest.setBurstingEnabled(true);

        log.info("Modifying Aliyun system disk attributes...");
        client.ecs().modifyDisk(modifyRequest);

        log.info("Aliyun system disk {} configured successfully.", resource.getName());
    }

    private void createDataDisk(Resource resource, AliyunDiskBuildInput input) {
        log.info("Creating Aliyun data disk: {}.", resource.getName());

        CreateDiskRequest request = toCreateDiskRequest(resource, input);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) diskHandler.getProvider();

        String diskId = provider.buildClient(account).ecs().createDisk(request);
        resource.setExternalId(diskId);

        log.info("Aliyun data disk {} created successfully. External ID: {}.", resource.getName(), diskId);
    }

    private CreateDiskRequest toCreateDiskRequest(Resource resource, AliyunDiskBuildInput input) {
        CreateDiskRequest request = new CreateDiskRequest();

        resolveBasicOptions(resource, input, request);

        resolvePlacement(resource, request);

        resolveAdvanceOptions(input, request);

        return request;
    }

    private void resolveAdvanceOptions(AliyunDiskBuildInput input, CreateDiskRequest request) {
        if(input.getEnableAdvanceOptions() == null || !input.getEnableAdvanceOptions())
            return;

        if(input.getDiskCategory() == AliyunDiskCategory.cloud_essd)
            request.setPerformanceLevel(input.getPerformanceLevel());

        request.setEncrypted(input.getEncrypted());

        request.setBurstingEnabled(input.getBurstPerformance());
    }

    private void resolvePlacement(Resource resource, CreateDiskRequest request) {
        Resource zone = resource.getEssentialTarget(ResourceCategories.ZONE).orElseThrow(
                () -> new StratoException("No zone found when creating disk.")
        );

        request.setZoneId(zone.getExternalId());
    }

    private void resolveBasicOptions(Resource resource, AliyunDiskBuildInput input, CreateDiskRequest request) {
        request.setDiskName(resource.getName());
        request.setSize(input.getDiskSize());
        request.setDiskCategory(input.getDiskCategory().getId());
    }


    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        AliyunDiskBuildInput input = JSON.convert(parameters, AliyunDiskBuildInput.class);

        return List.of(new ResourceUsage(
                UsageTypes.DISK_GB.type(),
                BigDecimal.valueOf(input.getDiskSize())
        ));
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        if(resource.isPrimaryTo(ResourceCategories.COMPUTE_INSTANCE))
            return;

        Optional<Resource> instance = resource.getExclusiveTarget(ResourceCategories.COMPUTE_INSTANCE);

        instance.ifPresent(i -> AliyunInstanceUtil.validateDiskCategoryMatchInstanceType(resource, i));
    }



    @Override
    public ResourceCost getActionCost(Resource resource, Map<String, Object> parameters) {
        if(resource.isPrimaryTo(ResourceCategories.COMPUTE_INSTANCE))
            return ResourceCost.ZERO;

        AliyunDiskBuildInput input = JSON.convert(parameters, AliyunDiskBuildInput.class);
        CreateDiskRequest createDiskRequest = toCreateDiskRequest(resource, input);

        var request = new DescribePriceRequest();

        request.setResourceType("disk");

        var dataDisk = new DescribePriceRequest.DescribePriceRequestDataDisk();

        dataDisk.setCategory(createDiskRequest.getDiskCategory());
        dataDisk.setSize(Long.valueOf(createDiskRequest.getSize()));
        dataDisk.setPerformanceLevel(createDiskRequest.getPerformanceLevel());

        request.setDataDisk(List.of(dataDisk));

        AliyunCloudProvider provider = (AliyunCloudProvider) diskHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        var priceInfo = provider.buildClient(account).ecs().describePrice(request).getPriceInfo();

        return new ResourceCost(priceInfo.getPrice().getTradePrice(), 1.0, ChronoUnit.HOURS);
    }
}
