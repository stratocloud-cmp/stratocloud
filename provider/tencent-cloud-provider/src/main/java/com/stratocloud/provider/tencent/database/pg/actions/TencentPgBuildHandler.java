package com.stratocloud.provider.tencent.database.pg.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.job.TaskState;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.constants.UsageTypes;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.database.pg.TencentPgHandler;
import com.stratocloud.provider.tencent.database.pg.requirements.TencentPgToClassHandler;
import com.stratocloud.provider.tencent.database.pg.requirements.TencentPgToVersionHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.postgres.v20170312.models.*;
import com.tencentcloudapi.vpc.v20170312.models.Subnet;
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
public class TencentPgBuildHandler implements BuildResourceActionHandler {

    private final TencentPgHandler pgHandler;

    public TencentPgBuildHandler(TencentPgHandler pgHandler) {
        this.pgHandler = pgHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return pgHandler;
    }

    @Override
    public String getTaskName() {
        return "创建PostgreSQL实例";
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return TencentPgBuildInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        if(resource == null || resource.getAccountId() == null)
            return Optional.empty();

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) pgHandler.getProvider();

        List<ZoneInfo> zones = provider.buildClient(account).describeAvailablePgZones();

        List<String> zoneIds = zones.stream().map(ZoneInfo::getZone).toList();
        List<String> zoneNames = zones.stream().map(ZoneInfo::getZoneName).toList();

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(TencentPgBuildInput.class);

        formMetaData = DynamicFormHelper.changeOptions(
                formMetaData,
                "slaveZone",
                zoneIds,
                zoneNames
        );

        return Optional.of(formMetaData);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) pgHandler.getProvider();
        TencentCloudClient client = provider.buildClient(account);

        CreateInstancesRequest request = getCreateInstancesRequest(resource, parameters, client);

        String instanceId = client.createPgInstance(request);
        resource.setExternalId(instanceId);
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        ResourceActionResult result = BuildResourceActionHandler.super.checkActionResult(resource, parameters);

        if(result.taskState() == TaskState.FINISHED){
            ResourceSyncScheduler.addSyncTask(
                    new ResourceSyncScheduler.SyncTask(
                            resource.getId(),
                            60L,
                            10
                    )
            );
        }

        return result;
    }

    private static CreateInstancesRequest getCreateInstancesRequest(Resource resource,
                                                                    Map<String, Object> parameters,
                                                                    TencentCloudClient client) {
        TencentPgBuildInput input = JSON.convert(parameters, TencentPgBuildInput.class);

        CreateInstancesRequest request = new CreateInstancesRequest();

        resolveBasics(request, resource, input);

        resolvePayment(request, input);

        resolvePlacement(request, resource, input, client);

        resolveVersion(request, resource, client);

        return request;
    }

    private static void resolveVersion(CreateInstancesRequest request,
                                       Resource resource,
                                       TencentCloudClient client) {
        Resource versionResource = resource.getEssentialTargetByType(TencentPgToVersionHandler.TYPE_ID).orElseThrow(
                () -> new StratoException("PG version not provided")
        );
        Version version = client.describePgVersion(versionResource.getExternalId()).orElseThrow(
                () -> new StratoException("PG version not found")
        );
        request.setDBEngine(version.getDBEngine());
        request.setDBMajorVersion(version.getDBMajorVersion());
    }

    private static void resolveBasics(CreateInstancesRequest request,
                                      Resource resource,
                                      TencentPgBuildInput input) {
        Resource classResource = resource.getEssentialTargetByType(TencentPgToClassHandler.TYPE_ID).orElseThrow(
                () -> new StratoException("PG class not provided")
        );

        request.setName(resource.getName());

        request.setSpecCode(classResource.getExternalId());
        request.setStorage(input.getStorage());
        request.setInstanceCount(1L);

        request.setSyncMode(input.getSyncMode());
        request.setCharset(input.getCharset());
        request.setAdminName(input.getAdminName());
        request.setAdminPassword(input.getAdminPassword());
    }

    private static void resolvePayment(CreateInstancesRequest request, TencentPgBuildInput input) {
        request.setInstanceChargeType(input.getInstanceChargeType());
        request.setAutoVoucher(input.getAutoVoucher());
        if(Objects.equals(input.getInstanceChargeType(), "PREPAID")){
            request.setPeriod(input.getPeriod());
            request.setAutoRenewFlag(input.getAutoRenewFlag());
        }else {
            request.setPeriod(1L);
        }
    }

    private static void resolvePlacement(CreateInstancesRequest request,
                                         Resource resource,
                                         TencentPgBuildInput input,
                                         TencentCloudClient client) {
        Resource zoneResource = resource.getEssentialTarget(ResourceCategories.ZONE).orElseThrow(
                () -> new StratoException("Zone not provided")
        );
        Resource subnetResource = resource.getEssentialTarget(ResourceCategories.SUBNET).orElseThrow(
                () -> new StratoException("Subnet not provided")
        );
        Subnet subnet = client.describeSubnet(subnetResource.getExternalId()).orElseThrow(
                () -> new StratoException("Subnet not found")
        );
        List<String> securityGroupIds = resource.getRequirementTargets(ResourceCategories.SECURITY_GROUP).stream().map(
                Resource::getExternalId
        ).toList();

        request.setZone(zoneResource.getExternalId());

        if(Utils.isNotBlank(input.getSlaveZone())){
            DBNode primary = new DBNode();
            primary.setRole("Primary");
            primary.setZone(zoneResource.getExternalId());

            DBNode standby = new DBNode();
            standby.setRole("Standby");
            standby.setZone(input.getSlaveZone());

            request.setDBNodeSet(new DBNode[] { primary, standby });
        }

        request.setVpcId(subnet.getVpcId());
        request.setSubnetId(subnet.getSubnetId());

        if(Utils.isNotEmpty(securityGroupIds))
            request.setSecurityGroupIds(securityGroupIds.toArray(String[]::new));
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) pgHandler.getProvider();
        TencentCloudClient client = provider.buildClient(account);

        TencentPgBuildInput input = JSON.convert(parameters, TencentPgBuildInput.class);

        Resource classResource = resource.getEssentialTargetByType(TencentPgToClassHandler.TYPE_ID).orElseThrow(
                () -> new StratoException("PG class not provided")
        );
        var instanceClass = client.describePgClass(classResource.getExternalId()).orElseThrow(
                () -> new StratoException("PG class not found")
        );


        return List.of(
                new ResourceUsage(
                        UsageTypes.CPU_CORES.type(),
                        BigDecimal.valueOf(instanceClass.detail().getCPU())
                ),
                new ResourceUsage(
                        UsageTypes.MEMORY_GB.type(),
                        BigDecimal.valueOf(instanceClass.detail().getMemory()/1024)
                ),
                new ResourceUsage(
                        UsageTypes.DISK_GB.type(),
                        BigDecimal.valueOf(input.getStorage())
                )
        );
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }

    @Override
    public ResourceCost getActionCost(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) pgHandler.getProvider();
        TencentCloudClient client = provider.buildClient(account);

        CreateInstancesRequest createInstancesRequest = getCreateInstancesRequest(resource, parameters, client);

        InquiryPriceCreateDBInstancesRequest request = new InquiryPriceCreateDBInstancesRequest();

        double timeAmount;
        ChronoUnit timeUnit;

        if(Objects.equals(createInstancesRequest.getInstanceChargeType(), "PREPAID")){
            request.setInstanceChargeType("PREPAID");
            request.setPeriod(1L);

            timeAmount = 1;
            timeUnit = ChronoUnit.MONTHS;
        } else {
            request.setInstanceChargeType("POSTPAID");
            request.setPeriod(1L);

            timeAmount = 1;
            timeUnit = ChronoUnit.HOURS;
        }

        request.setInstanceCount(1L);

        request.setZone(createInstancesRequest.getZone());
        request.setSpecCode(createInstancesRequest.getSpecCode());
        request.setDBEngine(createInstancesRequest.getDBEngine());
        request.setStorage(createInstancesRequest.getStorage());
        request.setInstanceType("primary");

        var response = client.describePgPrice(request);

        Long price = response.getPrice();

        if(price == null)
            return ResourceCost.ZERO;

        return new ResourceCost(price/100.0, timeAmount, timeUnit);
    }
}
