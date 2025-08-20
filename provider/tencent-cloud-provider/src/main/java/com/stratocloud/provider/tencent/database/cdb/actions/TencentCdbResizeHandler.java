package com.stratocloud.provider.tencent.database.cdb.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.*;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.job.TaskContext;
import com.stratocloud.provider.resource.ResourceActionHandler;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.resource.ResourceHandler;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.database.cdb.CdbArchitecture;
import com.stratocloud.provider.tencent.database.cdb.CdbDeviceType;
import com.stratocloud.provider.tencent.database.cdb.CdbUtil;
import com.stratocloud.provider.tencent.database.cdb.TencentCdbHandler;
import com.stratocloud.resource.*;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.TimeUtil;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.cdb.v20170320.models.*;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class TencentCdbResizeHandler implements ResourceActionHandler {

    private final TencentCdbHandler cdbHandler;

    public TencentCdbResizeHandler(TencentCdbHandler cdbHandler) {
        this.cdbHandler = cdbHandler;
    }

    @Override
    public ResourceHandler getResourceHandler() {
        return cdbHandler;
    }

    @Override
    public ResourceAction getAction() {
        return ResourceActions.RESIZE;
    }

    @Override
    public String getTaskName() {
        return "云数据库调整配置";
    }

    @Override
    public Set<ResourceState> getAllowedStates() {
        return ResourceState.getAliveStateSet().stream().filter(
                s -> s != ResourceState.SHUTDOWN
        ).collect(Collectors.toSet());
    }

    @Override
    public Optional<ResourceState> getTransitionState() {
        return Optional.of(ResourceState.CONFIGURING);
    }

    @Override
    public Class<? extends ResourceActionInput> getInputClass() {
        return ResizeInput.class;
    }

    @Override
    public Optional<DynamicFormMetaData> getDirectInputClassDynamicFormMetaData(Resource resource) {
        if(resource == null || Utils.isBlank(resource.getExternalId()))
            return Optional.empty();

        TencentCloudProvider provider = (TencentCloudProvider) cdbHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudClient client = provider.buildClient(account);

        Optional<InstanceInfo> cdb = cdbHandler.describeCdb(account, resource.getExternalId());

        if(cdb.isEmpty())
            return Optional.empty();

        DescribeCdbZoneConfigResponse zoneConfig = client.describeCdbZoneConfig();
        CdbZoneDataResult zoneDataResult = zoneConfig.getDataResult();

        if(zoneDataResult == null)
            return Optional.empty();

        CdbRegionSellConf[] regions = zoneDataResult.getRegions();
        if(Utils.isEmpty(regions))
            return Optional.empty();

        Optional<CdbRegionSellConf> currentRegionConf = Arrays.stream(regions).filter(
                r -> r.getRegion().equals(client.getRegion())
        ).findAny();

        if(currentRegionConf.isEmpty())
            return Optional.empty();

        CdbZoneSellConf[] zoneConfigs = currentRegionConf.get().getRegionConfig();
        CdbSellConfig[] sellConfigs = zoneDataResult.getConfigs();

        if(zoneConfigs == null)
            zoneConfigs = new CdbZoneSellConf[0];
        if(sellConfigs == null)
            sellConfigs = new CdbSellConfig[0];

        CdbDeviceType deviceType = CdbDeviceType.fromString(cdb.get().getDeviceType());

        Set<CdbDeviceType> supportedUpgradeDeviceTypes = switch (deviceType){
            case ECONOMICAL -> Set.of(CdbDeviceType.ECONOMICAL);
            case UNIVERSAL, EXCLUSIVE, UNKNOWN -> Set.of(CdbDeviceType.UNIVERSAL, CdbDeviceType.EXCLUSIVE);
            case BASIC_V2 -> Set.of(CdbDeviceType.BASIC_V2);
            case CLOUD_NATIVE_CLUSTER, CLOUD_NATIVE_CLUSTER_EXCLUSIVE -> Set.of(
                    CdbDeviceType.CLOUD_NATIVE_CLUSTER,
                    CdbDeviceType.CLOUD_NATIVE_CLUSTER_EXCLUSIVE
            );
        };

        List<String> zoneIds = Arrays.stream(zoneConfigs).map(CdbZoneSellConf::getZone).toList();
        List<String> zoneNames = Arrays.stream(zoneConfigs).map(CdbZoneSellConf::getZoneName).toList();

        List<CdbSellConfig> supportedSellConfigs = Arrays.stream(sellConfigs).filter(
                c -> supportedUpgradeDeviceTypes.contains(CdbDeviceType.fromString(c.getDeviceType()))
        ).toList();

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(ResizeInput.class);

        formMetaData = DynamicFormHelper.changeOptions(
                formMetaData,
                "sellConfigId",
                supportedSellConfigs.stream().map(c -> c.getId().toString()).toList(),
                supportedSellConfigs.stream().map(CdbUtil::getSellConfigName).toList()
        );

        formMetaData = DynamicFormHelper.changeOptions(
                formMetaData,
                "masterZone",
                zoneIds,
                zoneNames
        );

        formMetaData = DynamicFormHelper.changeOptions(
                formMetaData,
                "slaveZone",
                zoneIds,
                zoneNames
        );

        formMetaData = DynamicFormHelper.changeOptions(
                formMetaData,
                "backupZone",
                zoneIds,
                zoneNames
        );

        formMetaData = DynamicFormHelper.changeNestedFormFieldMetaData(
                formMetaData,
                "clusterTopology",
                CdbClusterTopology.getFormMetaData(zoneIds, zoneNames)
        );

        SlaveInfo slaveInfo = cdb.get().getSlaveInfo();

        String slaveZone = slaveInfo != null && slaveInfo.getFirst() != null ? slaveInfo.getFirst().getZone() : null;
        String backupZone = slaveInfo != null && slaveInfo.getSecond() != null ? slaveInfo.getSecond().getZone() : null;

        String sellConfigId = Arrays.stream(sellConfigs).filter(
                c -> CdbUtil.isSameSellConfig(c, cdb.get())
        ).findAny().map(c -> c.getId().toString()).orElse(null);

        ResizeInput input = new ResizeInput();
        input.setArchitecture(CdbArchitecture.fromCdb(cdb.get()));
        input.setSellConfigId(sellConfigId);
        input.setVolume(cdb.get().getVolume());
        input.setProtectMode(cdb.get().getProtectMode());
        input.setDeployMode(cdb.get().getDeployMode());
        input.setCrossCluster(false);
        input.setMasterZone(cdb.get().getZone());
        input.setSlaveZone(slaveZone);
        input.setBackupZone(backupZone);
        input.setClusterTopology(CdbClusterTopology.fromCdb(cdb.get()));

        formMetaData = DynamicFormHelper.changeDefaultValues(
                formMetaData,
                input
        );

        return Optional.of(formMetaData);
    }

    @Override
    public void run(Resource resource, Map<String, Object> parameters) {
        TencentCloudProvider provider = (TencentCloudProvider) cdbHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudClient client = provider.buildClient(account);

        ResizeInput input = JSON.convert(parameters, ResizeInput.class);

        CdbSellConfig sellConfig = client.describeCdbSellConfig(input.getSellConfigId()).orElseThrow(
                () -> new StratoException("Sell config not found")
        );

        UpgradeDBInstanceRequest request = new UpgradeDBInstanceRequest();
        request.setInstanceId(resource.getExternalId());

        request.setWaitSwitch(input.getWaitSwitch());

        request.setDeviceType(sellConfig.getDeviceType());
        request.setCpu(sellConfig.getCpu());
        request.setMemory(sellConfig.getMemory());

        request.setVolume(CdbUtil.getDiskSize(sellConfig, input.getVolume()));

        request.setProtectMode(input.getProtectMode());
        request.setDeployMode(input.getDeployMode());

        request.setCrossCluster(input.isCrossCluster() ? 1L : 0L);

        if(input.isCrossCluster())
            request.setZoneId(input.getMasterZone());

        if(CdbUtil.supportSlaveZone(input.getArchitecture())){
            request.setSlaveZone(input.getSlaveZone());
        }

        if(CdbUtil.supportBackupZone(input.getArchitecture())){
            request.setBackupZone(input.getBackupZone());
        }

        request.setDataCheckSensitive(input.getDataCheckSensitive());
        request.setMaxDelayTime(input.getMaxDelayTime());

        if(input.getArchitecture() == CdbArchitecture.CLUSTER){
            if(input.getClusterTopology() != null)
                request.setClusterTopology(input.getClusterTopology().toClusterTopology(input.getMasterZone()));
        }

        String asyncRequestId = client.upgradeCdb(request).getAsyncRequestId();

        TaskContext.setExternalTaskId(asyncRequestId);
    }

    @Override
    public ResourceActionResult checkActionResult(Resource resource, Map<String, Object> parameters) {
        return CdbUtil.checkAsyncRequestResult(resource);
    }

    @Override
    public List<ResourceUsage> predictUsageChangeAfterAction(Resource resource, Map<String, Object> parameters) {
        return List.of();
    }

    @Override
    public void validatePrecondition(Resource resource, Map<String, Object> parameters) {

    }

    @Override
    public ResourceCost getActionCost(Resource resource, Map<String, Object> parameters) {
        TencentCloudProvider provider = (TencentCloudProvider) cdbHandler.getProvider();
        ExternalAccount account = getAccountRepository().findExternalAccount(resource.getAccountId());
        TencentCloudClient client = provider.buildClient(account);

        var cdb = cdbHandler.describeCdb(account, resource.getExternalId()).orElseThrow(
                () -> new StratoException("CDB not found")
        );

        double timeAmount;
        ChronoUnit timeUnit;

        if (CdbUtil.isPrepaid(cdb)) {
            String deadlineTime = cdb.getDeadlineTime();
            if(deadlineTime != null)
                timeAmount = LocalDateTime.now().until(TimeUtil.fromString(deadlineTime), ChronoUnit.MONTHS) + 1;
            else
                timeAmount = 0;
            timeUnit = ChronoUnit.MONTHS;
        } else {
            timeAmount = 1.0;
            timeUnit = ChronoUnit.HOURS;
        }

        ResizeInput input = JSON.convert(parameters, ResizeInput.class);

        CdbSellConfig sellConfig = client.describeCdbSellConfig(input.getSellConfigId()).orElseThrow(
                () -> new StratoException("Sell config not found")
        );

        InquiryPriceUpgradeInstancesRequest request = new InquiryPriceUpgradeInstancesRequest();

        request.setInstanceId(cdb.getInstanceId());
        request.setMemory(sellConfig.getMemory());
        request.setVolume(input.getVolume());
        request.setCpu(sellConfig.getCpu());
        request.setProtectMode(input.getProtectMode());
        request.setDeviceType(sellConfig.getDeviceType());
        request.setInstanceNodes((long) input.getInstanceNodes());

        InquiryPriceUpgradeInstancesResponse response = client.inquiryPriceUpgradeCdb(request);

        return new ResourceCost(response.getPrice()/100.0, timeAmount, timeUnit);
    }

    @Data
    public static class ResizeInput implements ResourceActionInput {
        @InputField(label = "架构", disabled = true, conditions = "false")
        private CdbArchitecture architecture;

        @SelectField(
                label = "切换时间",
                options = {
                        "0",
                        "1"
                },
                optionNames = {
                        "立刻切换",
                        "维护时间窗内"
                },
                defaultValues = "1"
        )
        private Long waitSwitch;

        @SelectField(label = "规格")
        private String sellConfigId;

        @NumberField(
                label = "硬盘大小(GB)",
                min = 25,
                defaultValue = 200,
                conditions = "this.architecture !== 'ECONOMICAL_TWO_NODES'"
        )
        private Long volume;

        @SelectField(
                label = "数据复制方式",
                options = {
                        "0",
                        "1",
                        "2"
                },
                optionNames = {
                        "异步复制",
                        "半同步复制",
                        "强同步复制"
                },
                defaultValues = "1",
                conditions = "this.architecture !== 'ONE_NODE'"
        )
        private Long protectMode;
        @SelectField(
                label = "部署模式",
                options = {
                        "0",
                        "1"
                },
                optionNames = {
                        "单可用区部署",
                        "多可用区部署"
                },
                defaultValues = "0",
                conditions = "this.architecture !== 'ONE_NODE'"
        )
        private Long deployMode;
        @BooleanField(label = "跨区迁移")
        private boolean crossCluster;

        @SelectField(label = "主可用区", conditions = "this.crossCluster === true")
        private String masterZone;
        @SelectField(
                label = "备可用区",
                conditions = {
                        "this.deployMode === '1'",
                        "this.architecture === 'TWO_NODES' || this.architecture === 'ECONOMICAL_TWO_NODES'"
                }
        )
        private String slaveZone;
        @SelectField(
                label = "备可用区",
                conditions = {
                        "this.deployMode === '1'",
                        "this.architecture === 'THREE_NODES'"
                }
        )
        private String backupZone;

        @NestedFormField(
                label = "集群拓扑",
                nestedFormClass = CdbClusterTopology.class,
                conditions = "this.architecture === 'CLUSTER'"
        )
        private CdbClusterTopology clusterTopology;

        @NumberField(label = "数据校验延迟阈值(秒)", min = 1, max = 10, defaultValue = 10)
        private Long maxDelayTime;

        @SelectField(
                label = "数据校验敏感度",
                options = {
                        "high",
                        "normal",
                        "low"
                },
                optionNames = {
                        "高",
                        "标准",
                        "低"
                },
                defaultValues = "normal"
        )
        private String dataCheckSensitive;

        @JsonIgnore
        public int getInstanceNodes(){
            if(architecture == CdbArchitecture.ECONOMICAL_TWO_NODES || architecture == CdbArchitecture.TWO_NODES){
                return 2;
            }else if(architecture == CdbArchitecture.THREE_NODES){
                return 3;
            }else if(architecture == CdbArchitecture.ONE_NODE){
                return 1;
            }else if(architecture == CdbArchitecture.CLUSTER){
                if(clusterTopology != null)
                    return Utils.length(clusterTopology.getReadOnlyNodes()) + 1;
            }

            return 1;
        }
    }
}
