package com.stratocloud.provider.tencent.instance.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.job.TaskState;
import com.stratocloud.provider.RuntimePropertiesUtil;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.constants.UsageTypes;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.disk.actions.TencentDiskBuildInput;
import com.stratocloud.provider.tencent.flavor.TencentFlavorId;
import com.stratocloud.provider.tencent.instance.TencentInstanceHandler;
import com.stratocloud.provider.tencent.nic.actions.TencentNicBuildInput;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceActionResult;
import com.stratocloud.resource.ResourceCost;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.cbs.v20170312.models.Disk;
import com.tencentcloudapi.cvm.v20170312.models.*;
import com.tencentcloudapi.vpc.v20170312.models.NetworkInterface;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class TencentInstanceBuildHandler implements BuildResourceActionHandler {
    private final TencentInstanceHandler instanceHandler;

    public TencentInstanceBuildHandler(TencentInstanceHandler instanceHandler) {
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
        return TencentInstanceBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        RunInstancesRequest request = toRunInstancesRequest(resource, parameters);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) instanceHandler.getProvider();

        TencentCloudClient client = provider.buildClient(account);
        String instanceId = client.runInstance(request);

        resource.setExternalId(instanceId);
    }

    private RunInstancesRequest toRunInstancesRequest(Resource resource, Map<String, Object> parameters) {
        TencentInstanceBuildInput input = JSON.convert(parameters, TencentInstanceBuildInput.class);

        RunInstancesRequest request = new RunInstancesRequest();

        resolvePlacement(resource, request);

        resolveBasicOptions(resource, request);

        resolveChargeOptions(input, request);

        resolveSystemDisk(resource, request);

        resolvePrimaryNic(resource, request);

        resolveGuestOptions(resource, input, request);

        resolveHaOptions(resource, request);

        resolveSpecialClusterId(request);

        resolveApiOptions(input, request);

        return request;
    }

    private void setPrimaryNicId(Resource instance, TencentCloudClient client) {
        Optional<NetworkInterface> networkInterface = client.describePrimaryNicByInstanceId(instance.getExternalId());
        Optional<Resource> nicResource = instance.getPrimaryCapability(ResourceCategories.NIC);
        nicResource.ifPresent(nic -> nic.setExternalId(networkInterface.orElseThrow().getNetworkInterfaceId()));
    }

    private void setSystemDiskId(Resource instance, TencentCloudClient client) {
        Optional<Disk> disk = client.describeSystemDiskByInstanceId(instance.getExternalId());
        Optional<Resource> diskResource = instance.getPrimaryCapability(ResourceCategories.DISK);
        diskResource.ifPresent(d -> d.setExternalId(disk.orElseThrow().getDiskId()));
    }


    private void resolveApiOptions(TencentInstanceBuildInput input,
                                   RunInstancesRequest request) {
        boolean enableAdvanceOptions
                = input.getEnableAdvanceOptions() != null ? input.getEnableAdvanceOptions() : false;

        if(enableAdvanceOptions)
            request.setDisableApiTermination(input.getDisableApiTermination());

        request.setDryRun(false);
    }

    private void resolveSpecialClusterId(RunInstancesRequest request) {
        request.setHpcClusterId(null);
        request.setDedicatedClusterId(null);
        request.setChcIds(null);
    }

    private void resolveHaOptions(Resource resource, RunInstancesRequest request) {
        Optional<Resource> recoverGroup = resource.getExclusiveTarget(ResourceCategories.DISASTER_RECOVER_GROUP);

        if(recoverGroup.isEmpty())
            return;

        request.setDisasterRecoverGroupIds(new String[]{recoverGroup.get().getExternalId()});
    }

    private void resolveBasicOptions(Resource resource, RunInstancesRequest request) {
        Resource flavor = resource.getEssentialTarget(ResourceCategories.FLAVOR).orElseThrow(
                () -> new StratoException("No flavor found when creating instance.")
        );

        Resource image = resource.getEssentialTarget(ResourceCategories.IMAGE).orElseThrow(
                () -> new StratoException("No image found when creating instance.")
        );

        TencentFlavorId flavorId = TencentFlavorId.fromString(flavor.getExternalId());

        request.setInstanceType(flavorId.instanceType());
        request.setImageId(image.getExternalId());
        request.setInstanceName(resource.getName());
    }

    private void resolveGuestOptions(Resource resource,
                                     TencentInstanceBuildInput input,
                                     RunInstancesRequest request) {
        List<Resource> keyPairs = resource.getRequirementTargets(ResourceCategories.KEY_PAIR);

        boolean keepImageLogin = input.getKeepImageLogin() != null ? input.getKeepImageLogin() : false;

        LoginSettings loginSettings = new LoginSettings();
        loginSettings.setKeepImageLogin(keepImageLogin?"TRUE":null);

        if(Utils.isEmpty(keyPairs)) {
            loginSettings.setPassword(input.getPassword());
            RuntimePropertiesUtil.setManagementPassword(resource, input.getPassword());
        } else {
            loginSettings.setKeyIds(keyPairs.stream().map(Resource::getExternalId).toArray(String[]::new));
        }
        request.setLoginSettings(loginSettings);

        boolean enableAdvanceOptions
                = input.getEnableAdvanceOptions() != null ? input.getEnableAdvanceOptions() : false;

        if(enableAdvanceOptions){
            request.setEnhancedService(getEnhancedServiceOptions(input));
            request.setUserData(input.getUserData());
        }

        if(Utils.isNotBlank(input.getHostName()))
            request.setHostName(input.getHostName());
        else
            request.setHostName(resource.getName());
    }

    private static EnhancedService getEnhancedServiceOptions(TencentInstanceBuildInput input) {
        RunAutomationServiceEnabled automationServiceEnabled = new RunAutomationServiceEnabled();
        RunSecurityServiceEnabled securityServiceEnabled = new RunSecurityServiceEnabled();
        RunMonitorServiceEnabled monitorService = new RunMonitorServiceEnabled();

        automationServiceEnabled.setEnabled(input.getEnableAutomationService());
        securityServiceEnabled.setEnabled(input.getEnableSecurityService());
        monitorService.setEnabled(input.getEnableMonitorService());


        EnhancedService enhancedService = new EnhancedService();
        enhancedService.setAutomationService(automationServiceEnabled);
        enhancedService.setSecurityService(securityServiceEnabled);
        enhancedService.setMonitorService(monitorService);
        return enhancedService;
    }

    private void resolvePrimaryNic(Resource resource, RunInstancesRequest request) {
        Optional<Resource> primaryNic = resource.getPrimaryCapability(ResourceCategories.NIC);

        if(primaryNic.isEmpty())
            return;

        TencentNicBuildInput nicBuildInput = JSON.convert(
                primaryNic.get().getProperties(), TencentNicBuildInput.class
        );

        Resource subnet = primaryNic.get().getEssentialTarget(ResourceCategories.SUBNET).orElseThrow(
                () -> new StratoException("Subnet not found when creating nic.")
        );
        Resource vpc = subnet.getEssentialTarget(ResourceCategories.VPC).orElseThrow(
                () -> new StratoException("Vpc not found when creating nic.")
        );

        VirtualPrivateCloud virtualPrivateCloud = new VirtualPrivateCloud();
        virtualPrivateCloud.setVpcId(vpc.getExternalId());
        virtualPrivateCloud.setSubnetId(subnet.getExternalId());

        if(Utils.isNotEmpty(nicBuildInput.getIps()))
            virtualPrivateCloud.setPrivateIpAddresses(new String[]{nicBuildInput.getIps().get(0)});

        request.setVirtualPrivateCloud(virtualPrivateCloud);
    }

    private void resolveSystemDisk(Resource resource, RunInstancesRequest request) {
        Optional<Resource> systemDiskResource = resource.getPrimaryCapability(ResourceCategories.DISK);

        if(systemDiskResource.isEmpty())
            return;

        TencentDiskBuildInput diskBuildInput = JSON.convert(
                systemDiskResource.get().getProperties(), TencentDiskBuildInput.class
        );

        SystemDisk systemDisk = new SystemDisk();
        systemDisk.setDiskSize(diskBuildInput.getDiskSize());

        if(diskBuildInput.getDiskType()!=null)
            systemDisk.setDiskType(diskBuildInput.getDiskType().getId());

        request.setSystemDisk(systemDisk);
    }

    private void resolvePlacement(Resource resource, RunInstancesRequest request) {
        Resource zone = resource.getEssentialTarget(ResourceCategories.ZONE).orElseThrow(
                () -> new StratoException("No zone found when creating instance.")
        );

        Placement placement = new Placement();
        placement.setZone(zone.getExternalId());

        request.setPlacement(placement);
    }

    private void resolveChargeOptions(TencentInstanceBuildInput input, RunInstancesRequest request) {
        request.setInstanceChargeType(input.getChargeType());
        if(Objects.equals("PREPAID", input.getChargeType())){
            InstanceChargePrepaid prepaid = new InstanceChargePrepaid();
            prepaid.setPeriod(Long.valueOf(input.getPrepaidPeriod()));
            prepaid.setRenewFlag(input.getRenewType());
            request.setInstanceChargePrepaid(prepaid);
        }
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) instanceHandler.getProvider();

        List<ResourceUsage> result = new ArrayList<>();

        Optional<Resource> flavor = resource.getEssentialTarget(ResourceCategories.FLAVOR);

        if(flavor.isPresent()){
            Optional<InstanceTypeConfig> instanceTypeConfig = provider.buildClient(account).describeInstanceType(
                    TencentFlavorId.fromString(flavor.get().getExternalId())
            );

            if(instanceTypeConfig.isPresent()){
                ResourceUsage cpuUsage = new ResourceUsage(
                        UsageTypes.CPU_CORES.type(),
                        BigDecimal.valueOf(instanceTypeConfig.get().getCPU())
                );
                result.add(cpuUsage);

                ResourceUsage memoryUsage = new ResourceUsage(
                        UsageTypes.MEMORY_GB.type(),
                        BigDecimal.valueOf(instanceTypeConfig.get().getMemory())
                );
                result.add(memoryUsage);
            }
        }

        return result;
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        RunInstancesRequest request = toRunInstancesRequest(resource, parameters);
        request.setDryRun(true);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) instanceHandler.getProvider();

        TencentCloudClient client = provider.buildClient(account);
        client.runInstance(request);
    }


    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ResourceActionResult result = BuildResourceActionHandler.super.checkActionResult(resource, parameters);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) instanceHandler.getProvider();
        TencentCloudClient client = provider.buildClient(account);

        if(result.taskState() == TaskState.FINISHED){
            setPrimaryNicId(resource, client);
            setSystemDiskId(resource, client);
        }

        return result;
    }

    @Override
    public ResourceCost getActionCost(Resource resource, Map<String, Object> parameters) {
        RunInstancesRequest request = toRunInstancesRequest(resource, parameters);

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) instanceHandler.getProvider();

        InquiryPriceRunInstancesRequest inquiry = getInquiry(request);

        ItemPrice instancePrice = provider.buildClient(account).inquiryPriceRunInstance(inquiry).getInstancePrice();

        if(Objects.equals(request.getInstanceChargeType(), "PREPAID")) {
            long period = 1L;
            InstanceChargePrepaid prepaid = request.getInstanceChargePrepaid();
            if(prepaid != null && prepaid.getPeriod() != null)
                period = prepaid.getPeriod();
            return new ResourceCost(instancePrice.getDiscountPrice(), period, ChronoUnit.MONTHS);
        }else {
            return new ResourceCost(instancePrice.getUnitPriceDiscount(), 1.0f, ChronoUnit.HOURS);
        }

    }

    private static InquiryPriceRunInstancesRequest getInquiry(RunInstancesRequest request) {
        InquiryPriceRunInstancesRequest inquiry = new InquiryPriceRunInstancesRequest();
        inquiry.setPlacement(request.getPlacement());
        inquiry.setImageId(request.getImageId());
        inquiry.setInstanceChargeType(request.getInstanceChargeType());
        inquiry.setInstanceChargePrepaid(request.getInstanceChargePrepaid());
        inquiry.setInstanceType(request.getInstanceType());
        inquiry.setSystemDisk(request.getSystemDisk());
        inquiry.setDataDisks(request.getDataDisks());
        inquiry.setVirtualPrivateCloud(request.getVirtualPrivateCloud());
        inquiry.setInternetAccessible(request.getInternetAccessible());
        inquiry.setInstanceCount(request.getInstanceCount());
        inquiry.setInstanceName(request.getInstanceName());
        inquiry.setLoginSettings(request.getLoginSettings());
        inquiry.setSecurityGroupIds(request.getSecurityGroupIds());
        inquiry.setEnhancedService(request.getEnhancedService());
        inquiry.setHostName(request.getHostName());
        inquiry.setTagSpecification(request.getTagSpecification());
        inquiry.setInstanceMarketOptions(request.getInstanceMarketOptions());
        inquiry.setHpcClusterId(request.getHpcClusterId());
        inquiry.setLaunchTemplate(request.getLaunchTemplate());
        return inquiry;
    }
}
