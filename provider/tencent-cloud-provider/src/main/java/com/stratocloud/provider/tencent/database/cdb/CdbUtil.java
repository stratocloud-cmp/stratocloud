package com.stratocloud.provider.tencent.database.cdb;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.job.TaskContext;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.database.cdb.actions.CdbClusterTopology;
import com.stratocloud.provider.tencent.database.cdb.actions.CdbParamList;
import com.stratocloud.provider.tencent.database.cdb.actions.TencentCdbBuildInput;
import com.stratocloud.resource.Resource;
import com.stratocloud.resource.ResourceActionResult;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.cdb.v20170320.models.*;
import com.tencentcloudapi.vpc.v20170312.models.Subnet;

import java.util.*;

public class CdbUtil {
    public static boolean isSameSellConfig(CdbSellConfig sellConfig, InstanceInfo instanceInfo) {
        return Objects.equals(sellConfig.getDeviceType(), instanceInfo.getDeviceType()) &&
                Objects.equals(sellConfig.getEngineType(), instanceInfo.getEngineType()) &&
                Objects.equals(sellConfig.getCpu(), instanceInfo.getCpu()) &&
                Objects.equals(sellConfig.getMemory(), instanceInfo.getMemory());
    }

    public static String getSellConfigName(CdbSellConfig sellConfig) {
        CdbDeviceType deviceType = CdbDeviceType.fromString(sellConfig.getDeviceType());

        String diskDescription =
                deviceType == CdbDeviceType.ECONOMICAL ? " 硬盘大小:%sGB".formatted(sellConfig.getVolumeMax()) : "";

        return "实例类型:%s CPU:%s核 内存:%sMB 最大IOPS:%s 引擎类型:%s".formatted(
                deviceType == CdbDeviceType.UNKNOWN ? sellConfig.getDeviceType() : deviceType.getLabel(),
                sellConfig.getCpu(),
                sellConfig.getMemory(),
                sellConfig.getIops(),
                sellConfig.getEngineType()
        ) + diskDescription;
    }

    public static DynamicFormMetaData changeSellConfigOptions(DynamicFormMetaData formMetaData,
                                                              String key,
                                                              List<CdbSellConfig> sellConfigs){
        return DynamicFormHelper.changeOptions(
                formMetaData,
                key,
                sellConfigs.stream().map(c -> c.getId().toString()).toList(),
                sellConfigs.stream().map(CdbUtil::getSellConfigName).toList()
        );
    }

    public static void resolveInstanceRole(TencentCdbBuildInput input,
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

    public static void resolveArchitecture(TencentCdbBuildInput input,
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
        } else if(architecture == CdbArchitecture.TWO_NODES || architecture == CdbArchitecture.ECONOMICAL_TWO_NODES){
            request.setSlaveZone(input.getSlaveZone());

            hourRequest.setSlaveZone(input.getSlaveZone());

            zoneSet.add(input.getSlaveZone());
        }

        long deployMode = zoneSet.size() > 1 ? 1L : 0L;

        request.setDeployMode(deployMode);
        hourRequest.setDeployMode(deployMode);
    }

    public static void resolveDbSetting(TencentCdbBuildInput input,
                                        CreateDBInstanceRequest request,
                                        CreateDBInstanceHourRequest hourRequest) {
        request.setPort(input.getPort());
        request.setProtectMode(input.getProtectMode());
        request.setEngineVersion(input.getEngineVersion());
        request.setParamTemplateType(input.getParamTemplateType());
        request.setParamList(CdbParamList.getParamInfoList(input.getParams()));

        hourRequest.setPort(input.getPort());
        hourRequest.setProtectMode(input.getProtectMode());
        hourRequest.setEngineVersion(input.getEngineVersion());
        hourRequest.setParamTemplateType(input.getParamTemplateType());
        hourRequest.setParamList(CdbParamList.getParamInfoList(input.getParams()));

        if(Utils.isNotBlank(input.getPassword())){
            request.setPassword(input.getPassword());

            hourRequest.setPassword(input.getPassword());
        }
    }

    public static void resolvePlacement(Subnet subnet,
                                        CreateDBInstanceRequest request,
                                        CreateDBInstanceHourRequest hourRequest) {
        request.setZone(subnet.getZone());
        request.setUniqVpcId(subnet.getVpcId());
        request.setUniqSubnetId(subnet.getSubnetId());

        hourRequest.setZone(subnet.getZone());
        hourRequest.setUniqVpcId(subnet.getVpcId());
        hourRequest.setUniqSubnetId(subnet.getSubnetId());
    }

    public static void resolvePayment(TencentCdbBuildInput input,
                                      CreateDBInstanceRequest request) {
        request.setPeriod(input.getPrepaidPeriod());
        request.setAutoRenewFlag(input.isAutoRenew() ? 1L : 0L);
    }

    public static void resolveSellConfig(TencentCdbBuildInput input,
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

    public static long getDiskSize(CdbSellConfig sellConfig, TencentCdbBuildInput input){
        Long inputDiskSize = input.getDiskSize();

        return getDiskSize(sellConfig, inputDiskSize);
    }

    public static long getDiskSize(CdbSellConfig sellConfig, Long inputDiskSize) {
        CdbDeviceType deviceType = CdbDeviceType.fromString(sellConfig.getDeviceType());
        if(deviceType == CdbDeviceType.ECONOMICAL){
            return sellConfig.getVolumeMax() != null ? sellConfig.getVolumeMax() : 0L;
        }else {
            if(inputDiskSize == null)
                return 0L;

            Long volumeMin = sellConfig.getVolumeMin();
            Long volumeStep = sellConfig.getVolumeStep();

            return volumeMin + ((inputDiskSize - volumeMin) / volumeStep) * volumeStep;
        }
    }

    public static String getDiskType(TencentCdbBuildInput input){
        CdbArchitecture architecture = input.getArchitecture();
        if(architecture == CdbArchitecture.ONE_NODE || architecture == CdbArchitecture.CLUSTER)
            return input.getDiskType();

        return null;
    }

    public static boolean supportSlaveZone(CdbArchitecture architecture) {
        return Set.of(
                CdbArchitecture.TWO_NODES,
                CdbArchitecture.ECONOMICAL_TWO_NODES,
                CdbArchitecture.THREE_NODES
        ).contains(architecture);
    }


    public static boolean supportBackupZone(CdbArchitecture architecture) {
        return architecture == CdbArchitecture.THREE_NODES;
    }

    public static boolean isPrepaid(InstanceInfo instanceInfo){
        return Objects.equals(instanceInfo.getPayType(), 0L);
    }

    public static String getSupportedUpgradeEngineVersion(InstanceInfo instanceInfo) {
        if(instanceInfo.getEngineVersion() == null)
            return null;

        return switch (instanceInfo.getEngineVersion()){
            case "5.5" -> "5.6";
            case "5.6" -> "5.7";
            case "5.7" -> "8.0";
            default -> null;
        };
    }

    public static ResourceActionResult checkAsyncRequestResult(Resource resource) {
        Optional<String> taskId = TaskContext.getExternalTaskId();
        if(taskId.isEmpty())
            return ResourceActionResult.finished();

        ResourceHandler resourceHandler = resource.getResourceHandler();
        TencentCloudProvider provider = (TencentCloudProvider) resourceHandler.getProvider();
        ExternalAccount account = resourceHandler.getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudClient client = provider.buildClient(account);

        Optional<DescribeAsyncRequestInfoResponse> asyncRequest = client.describeCdbAsyncRequest(taskId.get());

        if(asyncRequest.isEmpty() || asyncRequest.get().getStatus() == null)
            return ResourceActionResult.finished();

        return switch (asyncRequest.get().getStatus()) {
            case "FAILED", "KILLED", "REMOVED", "PAUSED" -> ResourceActionResult.failed(asyncRequest.get().getInfo());
            case "INITIAL", "RUNNING" -> ResourceActionResult.inProgress();
            default -> ResourceActionResult.finished();
        };
    }
}
