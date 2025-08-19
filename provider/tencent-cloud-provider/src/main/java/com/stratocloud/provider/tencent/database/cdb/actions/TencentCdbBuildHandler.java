package com.stratocloud.provider.tencent.database.cdb.actions;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.constants.ResourceCategories;
import com.stratocloud.provider.constants.UsageTypes;
import com.stratocloud.provider.resource.BuildResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.database.cdb.*;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceCost;
import com.stratocloud.resource.ResourceUsage;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.cdb.v20170320.models.*;
import com.tencentcloudapi.vpc.v20170312.models.Subnet;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class TencentCdbBuildHandler implements BuildResourceActionHandler {

    private final TencentCdbHandler cdbHandler;

    public TencentCdbBuildHandler(TencentCdbHandler cdbHandler) {
        this.cdbHandler = cdbHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return cdbHandler;
    }

    @Override
    public String getTaskName() {
        return "创建云数据库";
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        if(resource == null || resource.getAccountId() == null)
            return Optional.empty();

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) cdbHandler.getProvider();
        return Optional.of(TencentCdbBuildInput.getFormMetaData(provider, account));
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return TencentCdbBuildInput.class;
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        String instanceId = createCdbInstance(resource, parameters, false);
        resource.setExternalId(instanceId);
    }

    private String createCdbInstance(Resource resource, Map<String, Object> parameters, boolean dryRun) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) cdbHandler.getProvider();
        TencentCloudClient client = provider.buildClient(account);

        TencentCdbBuildInput input = JSON.convert(parameters, TencentCdbBuildInput.class);
        Resource subnetResource = resource.getEssentialTarget(ResourceCategories.SUBNET).orElseThrow(
                () -> new StratoException("Subnet not provided")
        );
        Subnet subnet = client.describeSubnet(subnetResource.getExternalId()).orElseThrow(
                () -> new StratoException("Subnet not found")
        );

        CreateDBInstanceRequest request = new CreateDBInstanceRequest();
        request.setDryRun(dryRun);
        request.setInstanceName(resource.getName());
        request.setGoodsNum(1L);

        CreateDBInstanceHourRequest hourRequest = new CreateDBInstanceHourRequest();
        hourRequest.setDryRun(dryRun);
        hourRequest.setInstanceName(resource.getName());
        hourRequest.setGoodsNum(1L);

        resolveSellConfig(input, client, request, hourRequest);

        resolvePayment(input, request);

        resolvePlacement(subnet, request, hourRequest);

        resolveDbSetting(input, request, hourRequest);

        resolveArchitecture(input, subnet.getZone(), request, hourRequest);

        resolveInstanceRole(input, request, hourRequest);

        if(input.isPrepaid())
            return client.createCdbInstance(request);
        else
            return client.createCdbHourInstance(hourRequest);
    }

    private static void resolveInstanceRole(TencentCdbBuildInput input,
                                            CreateDBInstanceRequest request,
                                            CreateDBInstanceHourRequest hourRequest) {
        CdbInstanceRole role = input.getInstanceRole();

        request.setInstanceRole(role.name());

        hourRequest.setInstanceRole(role.name());

        if(role == CdbInstanceRole.dr || role == CdbInstanceRole.ro){
            var regionAndInstanceId = CdbRegionAndInstanceId.fromString(input.getMasterInstanceId());

            request.setMasterInstanceId(regionAndInstanceId.instanceId());
            request.setMasterRegion(regionAndInstanceId.regionId());

            hourRequest.setMasterInstanceId(regionAndInstanceId.instanceId());
            hourRequest.setMasterRegion(regionAndInstanceId.regionId());

            if(role == CdbInstanceRole.ro) {
                request.setRoGroup(input.getRoGroup().toRoGroup());

                hourRequest.setRoGroup(input.getRoGroup().toRoGroup());
            } if(role == CdbInstanceRole.dr) {
                request.setAutoSyncFlag(input.isAutoSync() ? 1L : 0L);

                hourRequest.setAutoSyncFlag(input.isAutoSync() ? 1L : 0L);
            }
        }



    }

    private static void resolveArchitecture(TencentCdbBuildInput input,
                                            String masterZone,
                                            CreateDBInstanceRequest request,
                                            CreateDBInstanceHourRequest hourRequest) {
        CdbArchitecture architecture = input.getArchitecture();

        Set<String> zoneSet = new HashSet<>();
        zoneSet.add(masterZone);

        if(architecture == CdbArchitecture.CLUSTER){
            CdbClusterTopology clusterTopology = input.getClusterTopology();
            if(clusterTopology != null){
                if(clusterTopology.getReadOnlyNodes() != null)
                    clusterTopology.getReadOnlyNodes().forEach(n -> zoneSet.add(n.getZone()));

                request.setClusterTopology(clusterTopology.toClusterTopology(masterZone));
                hourRequest.setClusterTopology(clusterTopology.toClusterTopology(masterZone));
            }
            request.setDataProtectVolume(input.getDataProtectVolume());
            hourRequest.setDataProtectVolume(input.getDataProtectVolume());
        } else if(architecture == CdbArchitecture.THREE_NODES){
            request.setSlaveZone(input.getSlaveZone());
            request.setBackupZone(input.getBackupZone());

            hourRequest.setSlaveZone(input.getSlaveZone());
            hourRequest.setBackupZone(input.getBackupZone());

            zoneSet.add(input.getSlaveZone());
            zoneSet.add(input.getBackupZone());
        } else if(architecture == CdbArchitecture.TWO_NODES){
            request.setSlaveZone(input.getSlaveZone());

            hourRequest.setSlaveZone(input.getSlaveZone());

            zoneSet.add(input.getSlaveZone());
        }

        long deployMode = zoneSet.size() > 1 ? 1L : 0L;

        request.setDeployMode(deployMode);
        hourRequest.setDeployMode(deployMode);
    }

    private static void resolveDbSetting(TencentCdbBuildInput input,
                                         CreateDBInstanceRequest request,
                                         CreateDBInstanceHourRequest hourRequest) {
        request.setPort(input.getPort());
        request.setEngineVersion(input.getEngineVersion());
        request.setParamTemplateType(input.getParamTemplateType());
        request.setParamList(CdbParamList.getParamInfoList(input.getParams()));

        hourRequest.setPort(input.getPort());
        hourRequest.setEngineVersion(input.getEngineVersion());
        hourRequest.setParamTemplateType(input.getParamTemplateType());
        hourRequest.setParamList(CdbParamList.getParamInfoList(input.getParams()));

        if(Utils.isNotBlank(input.getPassword())){
            request.setPassword(input.getPassword());

            hourRequest.setPassword(input.getPassword());
        }
    }

    private static void resolvePlacement(Subnet subnet,
                                         CreateDBInstanceRequest request,
                                         CreateDBInstanceHourRequest hourRequest) {
        request.setZone(subnet.getZone());
        request.setUniqVpcId(subnet.getVpcId());
        request.setUniqSubnetId(subnet.getSubnetId());

        hourRequest.setZone(subnet.getZone());
        hourRequest.setUniqVpcId(subnet.getVpcId());
        hourRequest.setUniqSubnetId(subnet.getSubnetId());
    }

    private static void resolvePayment(TencentCdbBuildInput input,
                                       CreateDBInstanceRequest request) {
        request.setPeriod(input.getPrepaidPeriod());
        request.setAutoRenewFlag(input.isAutoRenew() ? 1L : 0L);
    }

    private static void resolveSellConfig(TencentCdbBuildInput input,
                                          TencentCloudClient client,
                                          CreateDBInstanceRequest request,
                                          CreateDBInstanceHourRequest hourRequest) {
        CdbSellConfig sellConfig = client.describeCdbSellConfig(
                input.getSellConfigId().orElseThrow(
                        () -> new StratoException("Sell config id not provided")
                )
        ).orElseThrow(
                () -> new StratoException("Sell config not found")
        );

        request.setCpu(sellConfig.getCpu());
        request.setMemory(sellConfig.getMemory());
        request.setVolume(getDiskSize(sellConfig, input));
        request.setDiskType(getDiskType(input));
        request.setDeviceType(sellConfig.getDeviceType());
        request.setEngineType(sellConfig.getEngineType());

        hourRequest.setCpu(sellConfig.getCpu());
        hourRequest.setMemory(sellConfig.getMemory());
        hourRequest.setVolume(getDiskSize(sellConfig, input));
        hourRequest.setDiskType(getDiskType(input));
        hourRequest.setDeviceType(sellConfig.getDeviceType());
        hourRequest.setEngineType(sellConfig.getEngineType());
    }

    private static long getDiskSize(CdbSellConfig sellConfig, TencentCdbBuildInput input){
        CdbDeviceType deviceType = CdbDeviceType.fromString(sellConfig.getDeviceType());
        if(deviceType == CdbDeviceType.ECONOMICAL){
            return sellConfig.getVolumeMax() != null ? sellConfig.getVolumeMax() : 0L;
        }else {
            Long diskSize = input.getDiskSize();
            if(diskSize == null)
                return 0L;

            Long volumeMin = sellConfig.getVolumeMin();
            Long volumeStep = sellConfig.getVolumeStep();

            return volumeMin + ((diskSize - volumeMin) / volumeStep) * volumeStep;
        }
    }

    private static String getDiskType(TencentCdbBuildInput input){
        CdbArchitecture architecture = input.getArchitecture();
        if(architecture == CdbArchitecture.ONE_NODE || architecture == CdbArchitecture.CLUSTER)
            return input.getDiskType();

        return null;
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) cdbHandler.getProvider();
        TencentCloudClient client = provider.buildClient(account);

        TencentCdbBuildInput input = JSON.convert(parameters, TencentCdbBuildInput.class);

        Optional<String> sellConfigId = input.getSellConfigId();

        if(sellConfigId.isEmpty())
            return List.of();

        if(Utils.isBlank(sellConfigId.get()))
            return List.of();

        Optional<CdbSellConfig> sellConfig = client.describeCdbSellConfig(sellConfigId.get());

        return sellConfig.map(c -> List.of(
                new ResourceUsage(UsageTypes.CPU_CORES.type(), BigDecimal.valueOf(c.getCpu())),
                new ResourceUsage(UsageTypes.MEMORY_GB.type(), BigDecimal.valueOf(c.getMemory() / 1024.0)),
                new ResourceUsage(UsageTypes.DISK_GB.type(), BigDecimal.valueOf(getDiskSize(c, input)))
        )).orElseGet(List::of);
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {
        createCdbInstance(resource, parameters, true);
    }

    @Override
    public ResourceCost getActionCost(Resource resource, Map<String, Object> parameters) {
        Optional<Resource> zoneResource = resource.getEssentialTarget(ResourceCategories.ZONE);
        TencentCdbBuildInput input = JSON.convert(parameters, TencentCdbBuildInput.class);
        CdbInstanceRole instanceRole = input.getInstanceRole();

        if(zoneResource.isEmpty())
            return ResourceCost.ZERO;

        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudProvider provider = (TencentCloudProvider) cdbHandler.getProvider();
        TencentCloudClient client = provider.buildClient(account);

        DescribeDBPriceRequest request = new DescribeDBPriceRequest();

        double timeAmount;
        ChronoUnit timeUnit;

        if(input.isPrepaid()){
            request.setPayType("PRE_PAID");
            request.setPeriod(input.getPrepaidPeriod());

            timeAmount = input.getPrepaidPeriod();
            timeUnit = ChronoUnit.MONTHS;
        } else {
            request.setPayType("HOUR_PAID");
            request.setPeriod(1L);
            request.setLadder(3L);

            timeAmount = 1;
            timeUnit = ChronoUnit.HOURS;
        }

        request.setGoodsNum(1L);

        request.setZone(zoneResource.get().getExternalId());
        request.setInstanceRole(instanceRole == null ? CdbInstanceRole.master.name() : instanceRole.name());
        request.setInstanceNodes((long) input.getInstanceNodes());

        CdbSellConfig sellConfig = client.describeCdbSellConfig(
                input.getSellConfigId().orElseThrow(
                        () -> new StratoException("Sell config id not provided")
                )
        ).orElseThrow(
                () -> new StratoException("Sell config not found")
        );

        request.setDeviceType(sellConfig.getDeviceType());
        request.setDiskType(getDiskType(input));
        request.setMemory(sellConfig.getMemory());
        request.setVolume(getDiskSize(sellConfig, input));
        request.setCpu(sellConfig.getCpu());

        DescribeDBPriceResponse response = client.describeCdbPrice(request);

        Long price = response.getPrice();

        if(price == null)
            return ResourceCost.ZERO;

        return new ResourceCost(price/100.0, timeAmount, timeUnit);
    }
}
