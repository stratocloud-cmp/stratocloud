package com.stratocloud.provider.tencent.database.cdb.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.stratocloud.account.ExternalAccount;
import com.stratocloud.form.*;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.tencent.TencentCloudProvider;
import com.stratocloud.provider.tencent.common.TencentCloudClient;
import com.stratocloud.provider.tencent.common.TencentCloudRegion;
import com.stratocloud.provider.tencent.database.cdb.*;
import com.stratocloud.utils.Utils;
import com.tencentcloudapi.cdb.v20170320.models.*;
import lombok.Data;

import java.util.*;

@Data
public class TencentCdbBuildInput implements ResourceActionInput {
    @BooleanField(label = "是否预付费")
    private boolean prepaid;
    @SelectField(
            label = "购买时长",
            options = {
                    "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "24", "36"
            },
            optionNames = {
                    "1个月", "2个月", "3个月", "4个月", "5个月", "6个月", "7个月", "8个月", "9个月", "10个月", "11个月",
                    "1年", "2年", "3年"
            },
            conditions = "this.prepaid === true",
            defaultValues = "1"
    )
    private Long prepaidPeriod;
    @BooleanField(
            label = "自动续费",
            conditions = "this.prepaid === true"
    )
    private boolean autoRenew;

    @SelectField(
            label = "实例角色",
            options = {
                    "master",
                    "dr",
                    "ro"
            },
            optionNames = {
                    "主实例",
                    "灾备实例",
                    "只读实例"
            },
            defaultValues = "master"
    )
    private CdbInstanceRole instanceRole = CdbInstanceRole.master;
    @SelectField(
            label = "目标主实例",
            conditions = "this.instanceRole==='dr' || this.instanceRole==='ro'"
    )
    private String masterInstanceId;
    @NestedFormField(
            label = "只读实例组设置",
            nestedFormClass = CdbRoGroupInput.class,
            conditions = "this.instanceRole==='ro'"
    )
    private CdbRoGroupInput roGroup;
    @BooleanField(label = "自动发起灾备同步", conditions = "this.instanceRole==='dr'")
    private boolean autoSync;


    @SelectField(
            label = "架构",
            options = {
                    "TWO_NODES",
                    "ECONOMICAL_TWO_NODES",
                    "THREE_NODES",
                    "ONE_NODE",
                    "CLUSTER"
            },
            optionNames = {
                    "双节点",
                    "双节点(经济型)",
                    "三节点",
                    "单节点(云盘)",
                    "云盘版(云盘)"
            },
            defaultValues = "TWO_NODES",
            conditions = "this.instanceRole !== 'ro'"
    )
    private CdbArchitecture architecture;
    @SelectField(label = "规格", conditions = "this.architecture === 'ECONOMICAL_TWO_NODES'")
    private String economicalSellConfigId;
    @SelectField(
            label = "规格",
            conditions = "this.architecture === 'TWO_NODES' || this.architecture === 'THREE_NODES' || this.instanceRole === 'ro'"
    )
    private String multiNodesSellConfigId;
    @SelectField(label = "规格", conditions = "this.architecture === 'ONE_NODE'")
    private String oneNodeSellConfigId;
    @SelectField(label = "规格", conditions = "this.architecture === 'CLUSTER'")
    private String clusterSellConfigId;

    @NumberField(
            label = "数据保护空间(GB)",
            min = 1,
            max = 10,
            defaultValue = 1,
            conditions = "this.instanceRole !== 'ro' && this.architecture === 'CLUSTER'"
    )
    private Long dataProtectVolume;
    @NestedFormField(
            label = "集群拓扑",
            nestedFormClass = CdbClusterTopology.class,
            conditions = "this.instanceRole !== 'ro' && this.architecture === 'CLUSTER'"
    )
    private CdbClusterTopology clusterTopology;
    @SelectField(
            label = "备可用区",
            conditions = "this.instanceRole !== 'ro' && (this.architecture === 'ECONOMICAL_TWO_NODES' || this.architecture === 'TWO_NODES' || this.architecture === 'THREE_NODES')"
    )
    private String slaveZone;
    @SelectField(
            label = "备可用区",
            conditions = "this.instanceRole !== 'ro' && this.architecture === 'THREE_NODES'"
    )
    private String backupZone;

    @SelectField(
            label = "MySQL版本",
            options = {
                    "5.5",
                    "5.6",
                    "5.7",
                    "8.0"
            },
            defaultValues = "5.7"
    )
    private String engineVersion;

    @SelectField(
            label = "硬盘类型",
            options = {
                    "CLOUD_SSD",
                    "CLOUD_HSSD",
                    "CLOUD_PREMIUM"
            },
            optionNames = {
                    "SSD云硬盘",
                    "增强型SSD云硬盘",
                    "高性能云硬盘"
            },
            defaultValues = "CLOUD_HSSD",
            conditions = "this.architecture === 'ONE_NODE' || this.architecture === 'CLUSTER'"
    )
    private String diskType;

    @NumberField(label = "硬盘(GB)", defaultValue = 200, min = 25, conditions = "this.architecture !== 'ECONOMICAL_TWO_NODES'")
    private Long diskSize;


    @NumberField(label = "自定义端口", defaultValue = 3306, min = 1024, max = 65535)
    private Long port;

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
            label = "参数模板",
            options = {
                    "HIGH_STABILITY",
                    "HIGH_PERFORMANCE"
            },
            optionNames = {
                    "默认高稳定性模板",
                    "默认高性能模板"
            },
            defaultValues = "HIGH_STABILITY"
    )
    private String paramTemplateType;

    @NestedFormField(label = "自定义参数", nestedFormClass = CdbParamList.class)
    private Map<String, Object> params;

    @InputField(label = "root用户密码", inputType = "password", required = false)
    private String password;

    @JsonIgnore
    public Optional<String> getSellConfigId() {
        if(architecture == null)
            return Optional.empty();

        return switch (architecture) {
            case ECONOMICAL_TWO_NODES -> Optional.ofNullable(economicalSellConfigId);
            case TWO_NODES, THREE_NODES -> Optional.ofNullable(multiNodesSellConfigId);
            case ONE_NODE -> Optional.ofNullable(oneNodeSellConfigId);
            case CLUSTER -> Optional.ofNullable(clusterSellConfigId);
        };
    }

    public static DynamicFormMetaData getFormMetaData(TencentCloudProvider provider, ExternalAccount account) {
        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(TencentCdbBuildInput.class);

        TencentCloudClient client = provider.buildClient(account);

        DescribeCdbZoneConfigResponse zoneConfig = client.describeCdbZoneConfig();
        CdbZoneDataResult zoneDataResult = zoneConfig.getDataResult();

        if(zoneDataResult == null)
            return formMetaData;

        CdbRegionSellConf[] regions = zoneDataResult.getRegions();
        if(Utils.isEmpty(regions))
            return formMetaData;

        CdbRegionSellConf currentRegion = null;
        List<InstanceInfo> masterCdbInstances = new ArrayList<>();
        for (CdbRegionSellConf region : regions) {
            if(Objects.equals(region.getRegion(), client.getRegion()))
                currentRegion = region;

            Optional<TencentCloudRegion> tencentCloudRegion = TencentCloudRegion.fromId(region.getRegion());
            if(tencentCloudRegion.isEmpty())
                continue;

            TencentCloudClient regionClient = provider.buildClientWithRegion(account, tencentCloudRegion.get());
            DescribeDBInstancesRequest request = new DescribeDBInstancesRequest();
            request.setInstanceTypes(new Long[]{1L});
            List<InstanceInfo> instanceInfos = regionClient.describeCdbInstances(request);
            masterCdbInstances.addAll(instanceInfos);
        }

        List<CdbZoneSellConf> zones = new ArrayList<>();
        if(currentRegion != null && currentRegion.getRegionConfig() != null)
            zones.addAll(List.of(currentRegion.getRegionConfig()));

        List<String> zoneIds = zones.stream().map(CdbZoneSellConf::getZone).toList();
        List<String> zoneNames = zones.stream().map(CdbZoneSellConf::getZoneName).toList();

        List<String> masterInstanceIds = masterCdbInstances.stream().map(
                i -> new CdbRegionAndInstanceId(i.getRegion(), i.getInstanceId()).toString()
        ).toList();
        List<String> masterInstanceNames = masterCdbInstances.stream().map(InstanceInfo::getInstanceName).toList();

        formMetaData = DynamicFormHelper.changeOptions(
                formMetaData, "masterInstanceId", masterInstanceIds, masterInstanceNames
        );
        formMetaData = DynamicFormHelper.changeOptions(
                formMetaData, "slaveZone", zoneIds, zoneNames
        );
        formMetaData = DynamicFormHelper.changeOptions(
                formMetaData, "backupZone", zoneIds, zoneNames
        );

        formMetaData = DynamicFormHelper.changeNestedFormFieldMetaData(
                formMetaData, "clusterTopology", CdbClusterTopology.getFormMetaData(zoneIds, zoneNames)
        );

        formMetaData = changeSellConfigOptions(formMetaData, zoneDataResult);

        formMetaData = DynamicFormHelper.changeNestedFormFieldMetaData(
                formMetaData, "params", CdbParamList.getFormMetaData(
                        client, "HIGH_STABILITY", "InnoDB", "5.7"
                )
        );

        return formMetaData;
    }


    private static DynamicFormMetaData changeSellConfigOptions(DynamicFormMetaData formMetaData,
                                                               CdbZoneDataResult dataResult){
        if(Utils.isEmpty(dataResult.getConfigs()))
            return formMetaData;

        List<CdbSellConfig> economicalConfigs = new ArrayList<>();
        List<CdbSellConfig> multiNodeConfigs = new ArrayList<>();
        List<CdbSellConfig> oneNodeConfigs = new ArrayList<>();
        List<CdbSellConfig> clusterConfigs = new ArrayList<>();

        for (CdbSellConfig config : dataResult.getConfigs()) {
            CdbDeviceType deviceType = CdbDeviceType.fromString(config.getDeviceType());
            switch (deviceType){
                case ECONOMICAL -> economicalConfigs.add(config);
                case UNIVERSAL, EXCLUSIVE -> multiNodeConfigs.add(config);
                case BASIC_V2 -> oneNodeConfigs.add(config);
                case CLOUD_NATIVE_CLUSTER, CLOUD_NATIVE_CLUSTER_EXCLUSIVE -> clusterConfigs.add(config);
            }
        }

        formMetaData = CdbUtil.changeSellConfigOptions(formMetaData, "economicalSellConfigId", economicalConfigs);
        formMetaData = CdbUtil.changeSellConfigOptions(formMetaData, "multiNodesSellConfigId", multiNodeConfigs);
        formMetaData = CdbUtil.changeSellConfigOptions(formMetaData, "oneNodeSellConfigId", oneNodeConfigs);
        formMetaData = CdbUtil.changeSellConfigOptions(formMetaData, "clusterSellConfigId", clusterConfigs);

        return formMetaData;
    }

    @JsonIgnore
    public int getInstanceNodes(){
        if(instanceRole == CdbInstanceRole.ro)
            return 1;
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
