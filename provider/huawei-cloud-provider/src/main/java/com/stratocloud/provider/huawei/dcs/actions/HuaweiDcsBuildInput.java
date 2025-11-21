package com.stratocloud.provider.huawei.dcs.actions;

import com.huaweicloud.sdk.dcs.v2.model.AttrsObject;
import com.huaweicloud.sdk.dcs.v2.model.FlavorsItems;
import com.huaweicloud.sdk.dcs.v2.model.ListFlavorsRequest;
import com.huaweicloud.sdk.ecs.v2.model.NovaAvailabilityZone;
import com.stratocloud.form.*;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.huawei.common.HuaweiCloudClient;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.utils.Utils;
import lombok.Data;

import java.util.*;

@Data
public class HuaweiDcsBuildInput implements ResourceActionInput {
    @SelectField(
            label = "计费模式",
            options = {
                    "prePaid",
                    "postPaid"
            },
            optionNames = {
                    "预付费",
                    "后付费"
            },
            defaultValues = "postPaid"
    )
    private String chargingMode;

    @SelectField(
            label = "计费周期",
            options = {
                    "1", "2", "3", "4", "5", "6", "7", "8", "9", "12", "24", "36"
            },
            optionNames = {
                    "1个月", "2个月", "3个月", "4个月", "5个月", "6个月", "7个月", "8个月", "9个月", "1年", "2年", "3年"
            },
            conditions = "this.chargingMode === 'prePaid'",
            defaultValues = "1"
    )
    private Long period;

    @BooleanField(label = "自动续费", conditions = "this.chargingMode === 'prePaid'")
    private boolean autoRenew;

    @SelectField(
            label = "Redis版本",
            options = {
                    "4.0", "5.0", "6.0", "7.0"
            },
            optionNames = {
                    "4.0", "5.0", "6.0", "7.0"
            },
            defaultValues = "6.0"
    )
    private String engineVersion;

    @SelectField(
            label = "CPU架构",
            options = {
                    "x86_64",
                    "aarch64"
            },
            optionNames = {
                    "X86架构",
                    "ARM架构"
            },
            defaultValues = "x86_64"
    )
    private String cpuType;

    @SelectField(
            label = "实例类型",
            options = {
                    "single",
                    "ha",
                    "cluster",
                    "proxy",
                    "ha_rw_split"
            },
            optionNames = {
                    "单机",
                    "主备",
                    "Cluster集群",
                    "Proxy集群",
                    "读写分离"
            }
    )
    private String cacheMode;

    @SelectField(
            label = "分片数",
            conditions = "this.cacheMode === 'cluster' || this.cacheMode === 'proxy'"
    )
    private Long shardingCount;

    @SelectField(
            label = "副本数",
            conditions = "this.cacheMode === 'ha' || this.cacheMode === 'cluster' || this.cacheMode === 'ha_rw_split'"
    )
    private Long replicaCount;

    @SelectField(
            label = "实例规格",
            filterPredicates = {
                    "!formData.engineVersion || element.engineVersion.indexOf(formData.engineVersion) !== -1",
                    "!formData.cpuType || element.cpuType === formData.cpuType",
                    "!formData.cacheMode || element.cacheMode === formData.cacheMode",
                    "!formData.shardingCount || !element.shardingCount || element.shardingCount === formData.shardingCount",
                    "!formData.replicaCount || !element.replicaCount || element.replicaCount === formData.replicaCount"
            }
    )
    private String specCode;

    @SelectField(
            label = "缓存容量(GB)",
            filterPredicates = "element.specCodes && element.specCodes.indexOf(formData.specCode) !== -1"
    )
    private String capacity;

    @SelectField(
            label = "备可用区",
            conditions = "this.cacheMode && this.cacheMode !== 'single' && this.replicaCount !== '1'"
    )
    private String backupZone;

    @BooleanField(label = "免密码访问")
    private boolean noPassword;

    @InputField(label = "实例密码", conditions = "this.noPassword === false")
    private String password;

    @NestedFormField(
            label = "备份策略",
            nestedFormClass = BackupPolicyInput.class,
            conditions = "this.cacheMode && this.cacheMode !== 'single'"
    )
    private BackupPolicyInput backupPolicy;

    @NumberField(label = "实例端口", min = 1, max = 65535, defaultValue = 6379)
    private Integer port;



    public static DynamicFormMetaData getFormMetaData(HuaweiCloudClient client){
        ListFlavorsRequest request = new ListFlavorsRequest();
        request.setEngine("Redis");
        List<FlavorsItems> flavors = client.dcs().describeFlavors(request);

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(HuaweiDcsBuildInput.class);

        formMetaData = changeSpecCodeMeta(flavors, formMetaData);

        formMetaData = changeCapacityMeta(flavors, formMetaData);

        List<NovaAvailabilityZone> zones = client.ecs().describeZones();

        formMetaData = DynamicFormHelper.changeOptions(
                formMetaData,
                "backupZone",
                zones.stream().map(NovaAvailabilityZone::getZoneName).toList(),
                zones.stream().map(NovaAvailabilityZone::getZoneName).toList()
        );




        return formMetaData;
    }


    private static DynamicFormMetaData changeCapacityMeta(List<FlavorsItems> flavors, DynamicFormMetaData formMetaData) {
        Map<String, List<String>> capacityMappingSpecCodes = new HashMap<>();

        for (FlavorsItems flavor : flavors) {
            List<String> capacities = flavor.getCapacity();

            if(capacities != null){
                for (String capacity : capacities) {
                    capacityMappingSpecCodes.computeIfAbsent(
                            capacity,
                            c -> new ArrayList<>()
                    ).add(flavor.getSpecCode());
                }
            }
        }

        List<String> capacities = capacityMappingSpecCodes.keySet().stream().map(
                Float::valueOf
        ).sorted(
                Comparator.comparingDouble(k -> k)
        ).map(c -> c % 1 == 0 ? String.valueOf(c.intValue()) : c.toString()).toList();

        List<List<String>> specCodes = new ArrayList<>();

        for (String capacity : capacities) {
            specCodes.add(capacityMappingSpecCodes.get(capacity));
        }

        formMetaData = DynamicFormHelper.changeOptions(
                formMetaData,
                "capacity",
                capacities,
                capacities
        );

        formMetaData = DynamicFormHelper.addProperty(
                formMetaData,
                "capacity",
                "specCodes",
                "支持的实例规格",
                specCodes,
                false
        );
        return formMetaData;
    }


    private static DynamicFormMetaData changeSpecCodeMeta(List<FlavorsItems> flavors, DynamicFormMetaData formMetaData) {
        List<String> specCodes = flavors.stream().map(FlavorsItems::getSpecCode).toList();
        List<String> specCodeNames = flavors.stream().map(
                f -> {
                    if (Utils.isEmpty(f.getCapacity()))
                        return f.getSpecCode();
                    return "%s (%s)".formatted(
                            f.getSpecCode(),
                            String.join(",", f.getCapacity().stream().map(c -> c + "GB").toList())
                    );
                }
        ).toList();
        List<String> cpuTypes = flavors.stream().map(FlavorsItems::getCpuType).toList();
        List<String> engineVersions = flavors.stream().map(FlavorsItems::getEngineVersion).toList();
        List<String> cacheModes = flavors.stream().map(FlavorsItems::getCacheMode).toList();
        List<String> shardingNumbers = flavors.stream().map(
                f -> f.getAttrs().stream().filter(
                        attr -> Objects.equals(attr.getName(), "sharding_num")
                ).findAny().map(AttrsObject::getValue).orElse(null)
        ).toList();
        List<String> replicaNumbers = flavors.stream().map(
                f -> f.getReplicaCount() != null ? f.getReplicaCount().toString() : null
        ).toList();

        formMetaData = DynamicFormHelper.changeOptions(
                formMetaData,
                "specCode",
                specCodes,
                specCodeNames
        );
        formMetaData = DynamicFormHelper.addProperty(
                formMetaData,
                "specCode",
                "cpuType",
                "CPU架构",
                cpuTypes,
                false
        );
        formMetaData = DynamicFormHelper.addProperty(
                formMetaData,
                "specCode",
                "engineVersion",
                "Redis版本",
                engineVersions,
                false
        );
        formMetaData = DynamicFormHelper.addProperty(
                formMetaData,
                "specCode",
                "cacheMode",
                "实例类型",
                cacheModes,
                false
        );
        formMetaData = DynamicFormHelper.addProperty(
                formMetaData,
                "specCode",
                "shardingCount",
                "分片数量",
                shardingNumbers,
                false
        );
        formMetaData = DynamicFormHelper.addProperty(
                formMetaData,
                "specCode",
                "replicaCount",
                "副本数量",
                replicaNumbers,
                false
        );

        List<String> shardingOptions
                = shardingNumbers.stream().distinct().filter(Objects::nonNull).map(Integer::valueOf).filter(n -> n != 1).sorted().map(String::valueOf).toList();

        List<String> replicaOptions
                = replicaNumbers.stream().distinct().filter(Objects::nonNull).map(Integer::valueOf).sorted().map(String::valueOf).toList();

        formMetaData = DynamicFormHelper.changeOptions(
                formMetaData,
                "shardingCount",
                shardingOptions,
                shardingOptions
        );

        formMetaData = DynamicFormHelper.changeOptions(
                formMetaData,
                "replicaCount",
                replicaOptions,
                replicaOptions
        );

        return formMetaData;
    }
}
