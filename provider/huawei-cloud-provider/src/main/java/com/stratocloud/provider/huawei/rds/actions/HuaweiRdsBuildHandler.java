package com.stratocloud.provider.huawei.rds.actions;

import com.huaweicloud.sdk.rds.v3.model.*;
import com.huaweicloud.sdk.vpc.v2.model.Subnet;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.job.TaskContext;
import com.stratocloud.job.TaskState;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.constants.UsageTypes;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.common.HuaweiCloudClient;
import com.stratocloud.provider.huawei.rds.HuaweiRdsHandler;
import com.stratocloud.provider.huawei.rds.HuaweiRdsUtil;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceActionResult;
import com.stratocloud.resource.ResourceSyncScheduler;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.concurrent.SleepUtil;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class HuaweiRdsBuildHandler implements BuildResourceActionHandler {

    private final HuaweiRdsHandler rdsHandler;

    public HuaweiRdsBuildHandler(HuaweiRdsHandler rdsHandler) {
        this.rdsHandler = rdsHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return rdsHandler;
    }

    @Override
    public String getTaskName() {
        return "创建RDS实例";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return HuaweiRdsBuildInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        if(resource == null || resource.getAccountId() == null)
            return Optional.empty();

        HuaweiCloudProvider provider = (HuaweiCloudProvider) rdsHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        return Optional.of(
                HuaweiRdsBuildInput.getFormMetaData(provider.buildClient(account))
        );
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        CreateInstanceResponse response = createInstance(resource, parameters, false);
        String instanceId = response.getInstance().getId();
        resource.setExternalId(instanceId);

        SleepUtil.sleep(20);

        TaskContext.setExternalTaskId(response.getJobId());
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ResourceActionResult result = HuaweiRdsUtil.checkActionResult(resource);
        if(result.taskState() == TaskState.FINISHED)
            ResourceSyncScheduler.addSyncTask(
                    new ResourceSyncScheduler.SyncTask(
                            resource.getId(),
                            20L,
                            5
                    )
            );
        return result;
    }

    private CreateInstanceResponse createInstance(Resource resource, Map<String, Object> parameters, boolean dryRun) {
        HuaweiCloudProvider provider = (HuaweiCloudProvider) rdsHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudClient client = provider.buildClient(account);

        HuaweiRdsBuildInput input = JSON.convert(parameters, HuaweiRdsBuildInput.class);

        Resource zoneResource = resource.getEssentialTarget(ResourceCategories.ZONE).orElseThrow(
                () -> new StratoException("Zone not provided")
        );

        Resource subnetResource = resource.getEssentialTarget(ResourceCategories.SUBNET).orElseThrow(
                () -> new StratoException("Subnet not provided")
        );

        Subnet subnet = client.vpc().describeSubnet(subnetResource.getExternalId()).orElseThrow(
                () -> new StratoException("Subnet not found")
        );

        Resource securityGroupResource = resource.getEssentialTarget(ResourceCategories.SECURITY_GROUP).orElseThrow(
                () -> new StratoException("Security group not provided")
        );

        HuaweiRdsBuildInput.EngineInput engineInput = input.getEngineInput();

        InstanceRequest body = new InstanceRequest();
        body.setName(resource.getName());
        body.setDryRun(dryRun);
        body.setCount(1);

        ChargeInfo chargeInfo = new ChargeInfo();
        ChargeInfo.ChargeModeEnum chargeModeEnum = ChargeInfo.ChargeModeEnum.fromValue(input.getChargeMode());
        chargeInfo.setChargeMode(chargeModeEnum);
        if(Objects.equals(chargeModeEnum, ChargeInfo.ChargeModeEnum.PREPAID)){
            if(input.getPeriod() >= 12){
                chargeInfo.setPeriodType(ChargeInfo.PeriodTypeEnum.YEAR);
                chargeInfo.setPeriodNum(input.getPeriod().intValue() / 12);
            }else {
                chargeInfo.setPeriodType(ChargeInfo.PeriodTypeEnum.MONTH);
                chargeInfo.setPeriodNum(input.getPeriod().intValue());
            }
            chargeInfo.setIsAutoRenew(input.isAutoRenew());
        }
        chargeInfo.setIsAutoPay(true);
        body.setChargeInfo(chargeInfo);

        Datastore datastore = new Datastore();
        datastore.setType(Datastore.TypeEnum.fromValue(input.getEngine().name()));
        datastore.setVersion(engineInput.getEngineVersion());
        body.setDatastore(datastore);

        Ha ha = new Ha();
        Ha.ModeEnum modeEnum = Ha.ModeEnum.fromValue(engineInput.getHaMode());
        ha.setMode(modeEnum);
        if(Objects.equals(modeEnum, Ha.ModeEnum.HA)) {
            ha.setReplicationMode(Ha.ReplicationModeEnum.fromValue(engineInput.getReplicationMode()));

            body.setAvailabilityZone("%s,%s".formatted(zoneResource.getExternalId(), engineInput.getBackupZone()));
        }else {
            body.setAvailabilityZone(zoneResource.getExternalId());
        }
        body.setHa(ha);

        body.setConfigurationId(engineInput.getConfigurationId());
        body.setPort(String.valueOf(engineInput.getPort()));
        body.setPassword(engineInput.getPassword());
        body.setFlavorRef(engineInput.getFlavorCode());

        Volume volume = new Volume();
        volume.setType(Volume.TypeEnum.fromValue(engineInput.getStorageType()));
        volume.setSize(engineInput.getStorageSize());
        body.setVolume(volume);

        body.setVpcId(subnet.getVpcId());
        body.setSubnetId(subnet.getId());
        body.setSecurityGroupId(securityGroupResource.getExternalId());

        UnchangeableParam unchangeableParam = new UnchangeableParam();
        unchangeableParam.setLowerCaseTableNames(engineInput.isTableNameCaseSensitive()?"0":"1");
        body.setUnchangeableParam(unchangeableParam);

        CreateInstanceRequest request = new CreateInstanceRequest();
        request.setBody(body);

        return client.rds().createInstance(request);
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        HuaweiRdsBuildInput input = JSON.convert(parameters, HuaweiRdsBuildInput.class);

        HuaweiRdsBuildInput.EngineInput engineInput = input.getEngineInput();

        HuaweiCloudProvider provider = (HuaweiCloudProvider) rdsHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());

        Flavor flavor = provider.buildClient(account).rds().describeFlavor(
                ListFlavorsRequest.DatabaseNameEnum.fromValue(input.getEngine().name()),
                engineInput.getFlavorCode()
        ).orElseThrow(
                () -> new StratoException("Flavor not found")
        );

        return List.of(
                new ResourceUsage(
                        UsageTypes.CPU_CORES.type(),
                        new BigDecimal(flavor.getVcpus())
                ),
                new ResourceUsage(
                        UsageTypes.MEMORY_GB.type(),
                        new BigDecimal(flavor.getRam())
                ),
                new ResourceUsage(
                        UsageTypes.DISK_GB.type(),
                        new BigDecimal(engineInput.getStorageSize())
                )
        );
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        createInstance(resource, parameters, true);
    }
}
