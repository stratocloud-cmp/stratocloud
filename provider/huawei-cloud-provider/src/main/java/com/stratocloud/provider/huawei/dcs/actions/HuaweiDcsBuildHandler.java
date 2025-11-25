package com.stratocloud.provider.huawei.dcs.actions;

import com.huaweicloud.sdk.dcs.v2.model.*;
import com.huaweicloud.sdk.vpc.v2.model.Subnet;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.huawei.HuaweiCloudProvider;
import com.stratocloud.provider.huawei.common.HuaweiCloudClient;
import com.stratocloud.provider.huawei.dcs.HuaweiDcsHandler;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.TimeUtil;
import com.stratocloud.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class HuaweiDcsBuildHandler implements BuildResourceActionHandler {

    private final HuaweiDcsHandler dcsHandler;

    public HuaweiDcsBuildHandler(HuaweiDcsHandler dcsHandler) {
        this.dcsHandler = dcsHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return dcsHandler;
    }

    @Override
    public String getTaskName() {
        return "创建Redis实例";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return HuaweiDcsBuildInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudProvider provider = (HuaweiCloudProvider) dcsHandler.getProvider();
        DynamicFormMetaData formMetaData = HuaweiDcsBuildInput.getFormMetaData(provider.buildClient(account));
        return Optional.of(formMetaData);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        HuaweiDcsBuildInput input = JSON.convert(parameters, HuaweiDcsBuildInput.class);

        HuaweiCloudProvider provider = (HuaweiCloudProvider) dcsHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        HuaweiCloudClient client = provider.buildClient(account);

        CreateInstanceRequest request = new CreateInstanceRequest();
        CreateInstanceBody body = new CreateInstanceBody();

        BssParam bssParam = getBssParam(input);
        body.setBssParam(bssParam);

        body.setName(resource.getName());
        body.setDescription(resource.getDescription());
        body.setInstanceNum(1);

        body.setEngine("Redis");
        body.setEngineVersion(input.getEngineVersion());
        body.setSpecCode(input.getSpecCode());
        body.setCapacity(Float.valueOf(input.getCapacity()));

        SpecParam specParam = new SpecParam();
        specParam.setCacheMode(input.getCacheMode());
        if(Set.of("cluster", "proxy").contains(input.getCacheMode()))
            specParam.setShardingCount(input.getShardingCount().intValue());
        if(Set.of("ha", "cluster", "ha_rw_split").contains(input.getCacheMode()))
            specParam.setReplicaCount(input.getReplicaCount().intValue());
        body.setSpecParam(specParam);

        Resource zoneResource = resource.getEssentialTarget(ResourceCategories.ZONE).orElseThrow(
                () -> new StratoException("Zone not provided")
        );
        Resource subnetResource = resource.getEssentialTarget(ResourceCategories.SUBNET).orElseThrow(
                () -> new StratoException("Subnet not provided")
        );
        Subnet subnet = client.vpc().describeSubnet(subnetResource.getExternalId()).orElseThrow(
                () -> new StratoException("Subnet not found")
        );


        List<String> azCodes = new ArrayList<>();
        azCodes.add(zoneResource.getExternalId());

        if(!Objects.equals(input.getCacheMode(), "single") &&
                !Objects.equals(input.getReplicaCount(), 1L) &&
                Utils.isNotBlank(input.getBackupZone()))
            azCodes.add(input.getBackupZone());
        body.setAzCodes(azCodes);
        body.setVpcId(subnet.getVpcId());
        body.setSubnetId(subnet.getId());

        body.setPort(input.getPort());
        body.setNoPasswordAccess(input.isNoPassword());
        if(!input.isNoPassword())
            body.setPassword(input.getPassword());

        if(!Objects.equals(input.getCacheMode(), "single")){
            BackupPolicy policy = new BackupPolicy();
            policy.setBackupType(input.getBackupPolicy().getBackupType());

            if(Objects.equals(input.getBackupPolicy().getBackupType(), "auto")){
                policy.setSaveDays(input.getBackupPolicy().getSaveDays());

                BackupPlan plan = new BackupPlan();
                plan.setBackupAt(input.getBackupPolicy().getBackupAt().stream().map(Long::intValue).toList());
                plan.setPeriodType("weekly");
                plan.setBeginAt(TimeUtil.toUtcTimeRange(input.getBackupPolicy().getBeginAt()));
                policy.setPeriodicalBackupPlan(plan);
            }


            body.setInstanceBackupPolicy(policy);
        }


        request.setBody(body);

        String instanceId = client.dcs().createInstance(request);
        resource.setExternalId(instanceId);
    }

    private static BssParam getBssParam(HuaweiDcsBuildInput input) {
        BssParam bssParam = new BssParam();

        if(Objects.equals(input.getChargingMode(), "prePaid")){
            bssParam.setChargingMode(BssParam.ChargingModeEnum.PREPAID);
            bssParam.setIsAutoRenew(input.isAutoRenew() ? BssParam.IsAutoRenewEnum.TRUE : BssParam.IsAutoRenewEnum.FALSE);

            Long period = input.getPeriod();

            if(period >= 12){
                bssParam.setPeriodType(BssParam.PeriodTypeEnum.YEAR);
                bssParam.setPeriodNum(period.intValue() / 12);
            }else {
                bssParam.setPeriodType(BssParam.PeriodTypeEnum.MONTH);
                bssParam.setPeriodNum(period.intValue());
            }
        }else {
            bssParam.setChargingMode(BssParam.ChargingModeEnum.POSTPAID);
        }

        bssParam.setIsAutoPay(BssParam.IsAutoPayEnum.TRUE);
        return bssParam;
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }
}
