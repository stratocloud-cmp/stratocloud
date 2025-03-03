package com.stratocloud.provider.aliyun.instance.actions;

import com.aliyun.ecs20140526.models.DescribePriceRequest;
import com.aliyun.ecs20140526.models.RunInstancesRequest;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.job.TaskState;
import com.stratocloud.provider.RuntimePropertiesUtil;
import com.stratocloud.provider.aliyun.AliyunCloudProvider;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.aliyun.disk.AliyunDisk;
import com.stratocloud.provider.aliyun.disk.actions.AliyunDiskBuildInput;
import com.stratocloud.provider.aliyun.flavor.AliyunFlavor;
import com.stratocloud.provider.aliyun.flavor.AliyunFlavorId;
import com.stratocloud.provider.aliyun.instance.AliyunInstanceHandler;
import com.stratocloud.provider.aliyun.instance.AliyunInstanceUtil;
import com.stratocloud.provider.aliyun.nic.AliyunNic;
import com.stratocloud.provider.aliyun.nic.actions.AliyunNicBuildInput;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.constants.UsageTypes;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceActionResult;
import com.stratocloud.resource.ResourceCost;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class AliyunInstanceBuildHandler implements BuildResourceActionHandler {
    private final AliyunInstanceHandler instanceHandler;

    public AliyunInstanceBuildHandler(AliyunInstanceHandler instanceHandler) {
        this.instanceHandler = instanceHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return instanceHandler;
    }

    @Override
    public String getTaskName() {
        return "创建云主机";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return AliyunInstanceBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        RunInstancesRequest request = toRunInstancesRequest(resource, parameters);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) instanceHandler.getProvider();

        AliyunClient client = provider.buildClient(account);
        String instanceId = client.ecs().runInstance(request);

        resource.setExternalId(instanceId);
    }

    private RunInstancesRequest toRunInstancesRequest(Resource resource, Map<String, Object> parameters) {
        AliyunInstanceBuildInput input = JSON.convert(parameters, AliyunInstanceBuildInput.class);

        RunInstancesRequest request = new RunInstancesRequest();

        resolvePlacement(resource, request);

        resolveBasicOptions(resource, request);

        resolveChargeOptions(input, request);

        resolveSystemDisk(resource, request);

        resolvePrimaryNic(resource, request);

        resolveGuestOptions(resource, input, request);

        resolveSpecialClusterId(request);

        resolveApiOptions(input, request);

        return request;
    }

    private void setPrimaryNicId(Resource instance, AliyunClient client) {
        Optional<AliyunNic> networkInterface = client.ecs().describePrimaryNicByInstanceId(instance.getExternalId());
        Optional<Resource> nicResource = instance.getPrimaryCapability(ResourceCategories.NIC);
        nicResource.ifPresent(nic -> nic.setExternalId(networkInterface.orElseThrow().detail().getNetworkInterfaceId()));
    }

    private void setSystemDiskId(Resource instance, AliyunClient client) {
        Optional<AliyunDisk> disk = client.ecs().describeSystemDiskByInstanceId(instance.getExternalId());
        Optional<Resource> diskResource = instance.getPrimaryCapability(ResourceCategories.DISK);
        diskResource.ifPresent(d -> d.setExternalId(disk.orElseThrow().detail().getDiskId()));
    }


    private void resolveApiOptions(AliyunInstanceBuildInput input,
                                   RunInstancesRequest request) {
        boolean enableAdvanceOptions
                = input.getEnableAdvanceOptions() != null ? input.getEnableAdvanceOptions() : false;

        if(enableAdvanceOptions)
            request.setDeletionProtection(input.getDeletionProtection());

        request.setDryRun(false);
    }

    private void resolveSpecialClusterId(RunInstancesRequest request) {
        request.setHpcClusterId(null);
    }


    private void resolveBasicOptions(Resource resource, RunInstancesRequest request) {
        Resource flavor = resource.getEssentialTarget(ResourceCategories.FLAVOR).orElseThrow(
                () -> new StratoException("No flavor found when creating instance.")
        );

        Resource image = resource.getEssentialTarget(ResourceCategories.IMAGE).orElseThrow(
                () -> new StratoException("No image found when creating instance.")
        );

        AliyunFlavorId flavorId = AliyunFlavorId.fromString(flavor.getExternalId());

        request.setInstanceType(flavorId.instanceTypeId());
        request.setImageId(image.getExternalId());
        request.setInstanceName(resource.getName());
    }

    private void resolveGuestOptions(Resource resource,
                                     AliyunInstanceBuildInput input,
                                     RunInstancesRequest request) {
        Optional<Resource> keyPair = resource.getExclusiveTarget(ResourceCategories.KEY_PAIR);

        boolean passwordInherit = input.getPasswordInherit() != null ? input.getPasswordInherit() : false;

        request.setPasswordInherit(passwordInherit);

        if(keyPair.isPresent())
            request.setKeyPairName(keyPair.get().getName());
        else if(!passwordInherit && Utils.isNotBlank(input.getPassword())) {
            request.setPassword(input.getPassword());
            RuntimePropertiesUtil.setManagementPassword(resource, input.getPassword());
        }


        boolean enableAdvanceOptions
                = input.getEnableAdvanceOptions() != null ? input.getEnableAdvanceOptions() : false;

        if(enableAdvanceOptions) {
            if(Utils.isNotBlank(input.getUserData()))
                request.setUserData(input.getUserData());
            if(Utils.isNotBlank(input.getSecurityEnhancementStrategy()))
                request.setSecurityEnhancementStrategy(input.getSecurityEnhancementStrategy());
        }


        if(Utils.isNotBlank(input.getHostName()))
            request.setHostName(input.getHostName());
        else
            request.setHostName(resource.getName());
    }
    private void resolvePrimaryNic(Resource resource, RunInstancesRequest request) {
        Optional<Resource> primaryNic = resource.getPrimaryCapability(ResourceCategories.NIC);

        if(primaryNic.isEmpty())
            return;

        AliyunNicBuildInput nicBuildInput = JSON.convert(
                primaryNic.get().getProperties(), AliyunNicBuildInput.class
        );

        Resource subnet = primaryNic.get().getEssentialTarget(ResourceCategories.SUBNET).orElseThrow(
                () -> new StratoException("Subnet not found when creating nic.")
        );

        List<Resource> securityGroups = primaryNic.get().getRequirementTargets(ResourceCategories.SECURITY_GROUP);

        var primaryNicRequest = new RunInstancesRequest.RunInstancesRequestNetworkInterface();

        primaryNicRequest.setVSwitchId(subnet.getExternalId());

        if(Utils.isNotEmpty(nicBuildInput.getIps()))
            primaryNicRequest.setPrimaryIpAddress(nicBuildInput.getIps().get(0));

        primaryNicRequest.setInstanceType("Primary");
        primaryNicRequest.setSecurityGroupIds(securityGroups.stream().map(Resource::getExternalId).toList());

        request.setNetworkInterface(List.of(primaryNicRequest));
    }

    private void resolveSystemDisk(Resource resource, RunInstancesRequest request) {
        Optional<Resource> systemDiskResource = resource.getPrimaryCapability(ResourceCategories.DISK);

        if(systemDiskResource.isEmpty())
            return;

        AliyunDiskBuildInput diskBuildInput = JSON.convert(
                systemDiskResource.get().getProperties(), AliyunDiskBuildInput.class
        );


        var systemDisk = new RunInstancesRequest.RunInstancesRequestSystemDisk();


        systemDisk.setSize(diskBuildInput.getDiskSize().toString());

        systemDisk.setCategory(diskBuildInput.getDiskCategory().getId());
        systemDisk.setDiskName(systemDiskResource.get().getName());

        systemDisk.setSize(diskBuildInput.getDiskSize().toString());

        if(diskBuildInput.getEnableAdvanceOptions() != null && diskBuildInput.getEnableAdvanceOptions()){
            systemDisk.setPerformanceLevel(diskBuildInput.getPerformanceLevel());
            systemDisk.setBurstingEnabled(diskBuildInput.getBurstPerformance());

            if(diskBuildInput.getEncrypted() != null && diskBuildInput.getEncrypted())
                systemDisk.setEncrypted("true");
        }

        request.setSystemDisk(systemDisk);
    }

    private void resolvePlacement(Resource resource, RunInstancesRequest request) {
        Resource zone = resource.getEssentialTarget(ResourceCategories.ZONE).orElseThrow(
                () -> new StratoException("No zone found when creating instance.")
        );

        request.setZoneId(zone.getExternalId());
    }

    private void resolveChargeOptions(AliyunInstanceBuildInput input, RunInstancesRequest request) {
        request.setInstanceChargeType(input.getChargeType());
        if(Objects.equals("PrePaid", input.getChargeType())){
            request.setPeriod(input.getPrepaidPeriod());
            request.setAutoRenew(input.getAutoRenew());
            request.setAutoRenewPeriod(input.getAutoRenewPeriod());
        }
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) instanceHandler.getProvider();

        List<ResourceUsage> result = new ArrayList<>();

        Optional<Resource> flavor = resource.getEssentialTarget(ResourceCategories.FLAVOR);

        if(flavor.isPresent()){
            Optional<AliyunFlavor> aliyunFlavor = provider.buildClient(account).ecs().describeFlavor(
                    AliyunFlavorId.fromString(flavor.get().getExternalId())
            );


            if(aliyunFlavor.isPresent()){
                ResourceUsage cpuUsage = new ResourceUsage(
                        UsageTypes.CPU_CORES.type(),
                        BigDecimal.valueOf(aliyunFlavor.get().detail().getCpuCoreCount())
                );
                result.add(cpuUsage);

                ResourceUsage memoryUsage = new ResourceUsage(
                        UsageTypes.MEMORY_GB.type(),
                        BigDecimal.valueOf(aliyunFlavor.get().detail().getMemorySize())
                );
                result.add(memoryUsage);
            }
        }

        return result;
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        validatePrimaryNic(resource);

        validateSystemDisk(resource);

        RunInstancesRequest request = toRunInstancesRequest(resource, parameters);
        request.setDryRun(true);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) instanceHandler.getProvider();

        AliyunClient client = provider.buildClient(account);
        client.ecs().runInstance(request);
    }

    private void validateSystemDisk(Resource instance) {
        Optional<Resource> systemDisk = instance.getPrimaryCapability(ResourceCategories.DISK);

        if(systemDisk.isEmpty())
            return;

        AliyunInstanceUtil.validateDiskCategoryMatchInstanceType(systemDisk.get(), instance);
    }

    private void validatePrimaryNic(Resource resource) {
        Optional<Resource> primaryNic = resource.getPrimaryCapability(ResourceCategories.NIC);

        if(primaryNic.isEmpty())
            return;

        List<Resource> securityGroups = primaryNic.get().getRequirementTargets(ResourceCategories.SECURITY_GROUP);

        if(Utils.isEmpty(securityGroups))
            throw new BadCommandException("请为主网卡至少指定一个安全组");
    }


    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ResourceActionResult result = BuildResourceActionHandler.super.checkActionResult(resource, parameters);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) instanceHandler.getProvider();
        AliyunClient client = provider.buildClient(account);

        if(result.taskState() == TaskState.FINISHED){
            setPrimaryNicId(resource, client);
            setSystemDiskId(resource, client);
        }

        return result;
    }

    @Override
    public ResourceCost getActionCost(Resource resource, Map<String, Object> parameters) {
        RunInstancesRequest runInstancesRequest = toRunInstancesRequest(resource, parameters);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        AliyunCloudProvider provider = (AliyunCloudProvider) instanceHandler.getProvider();

        AliyunClient client = provider.buildClient(account);

        var request = new DescribePriceRequest();
        request.setZoneId(runInstancesRequest.getZoneId());
        request.setInstanceType(runInstancesRequest.getInstanceType());

        if("PrePaid".equalsIgnoreCase(runInstancesRequest.getInstanceChargeType())){
            Integer period = runInstancesRequest.getPeriod();
            if(period<=9){
                request.setPriceUnit("Month");
                request.setPeriod(period);
            }else {
                request.setPriceUnit("Year");
                request.setPeriod(period/12);
            }
        }else {
            request.setPeriod(1);
            request.setPriceUnit("Hour");
        }


        var requestSystemDisk = runInstancesRequest.getSystemDisk();

        if(requestSystemDisk != null){
            var systemDisk = new DescribePriceRequest.DescribePriceRequestSystemDisk();
            systemDisk.setSize(Integer.valueOf(requestSystemDisk.getSize()));
            systemDisk.setCategory(requestSystemDisk.getCategory());
            systemDisk.setPerformanceLevel(requestSystemDisk.getPerformanceLevel());
            request.setSystemDisk(systemDisk);
        }

        var requestDataDisks = runInstancesRequest.getDataDisk();

        if(Utils.isNotEmpty(requestDataDisks)){
            var dataDisks = getDescribePriceRequestDataDisks(requestDataDisks);

            if(!dataDisks.isEmpty())
                request.setDataDisk(dataDisks);
        }


        var priceInfo = client.ecs().describePrice(request).getPriceInfo();
        Float tradePrice = priceInfo.getPrice().getTradePrice();

        if("PrePaid".equalsIgnoreCase(runInstancesRequest.getInstanceChargeType())){
            Integer period = runInstancesRequest.getPeriod();
            return new ResourceCost(tradePrice, period, ChronoUnit.MONTHS);
        }else {
            return new ResourceCost(tradePrice, 1.0, ChronoUnit.HOURS);
        }
    }

    private static List<DescribePriceRequest.DescribePriceRequestDataDisk> getDescribePriceRequestDataDisks(List<RunInstancesRequest.RunInstancesRequestDataDisk> requestDataDisks) {
        List<DescribePriceRequest.DescribePriceRequestDataDisk> dataDisks = new ArrayList<>();
        for (var requestDataDisk : requestDataDisks) {
            var dataDisk = new DescribePriceRequest.DescribePriceRequestDataDisk();
            dataDisk.setSize(Long.valueOf(requestDataDisk.getSize()));
            dataDisk.setCategory(requestDataDisk.getCategory());
            dataDisk.setPerformanceLevel(requestDataDisk.getPerformanceLevel());
            dataDisks.add(dataDisk);
        }
        return dataDisks;
    }
}
