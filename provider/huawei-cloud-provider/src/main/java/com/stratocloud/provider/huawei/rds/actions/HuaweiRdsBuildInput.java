package com.stratocloud.provider.huawei.rds.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.huaweicloud.sdk.ecs.v2.model.NovaAvailabilityZone;
import com.huaweicloud.sdk.rds.v3.model.*;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.*;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.constants.DbEngine;
import com.stratocloud.provider.huawei.common.HuaweiCloudClient;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.utils.Utils;
import com.stratocloud.utils.concurrent.RealTimeTaskUtil;
import lombok.Data;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Future;

@Data
public class HuaweiRdsBuildInput implements ResourceActionInput {
    @SelectField(
            label = "计费模式",
            options = {
                    "prePaid",
                    "postPaid"
            },
            optionNames = {
                    "包年包月",
                    "按量计费"
            },
            defaultValues = "postPaid"
    )
    private String chargeMode;

    @SelectField(
            label = "购买时长",
            options = {
                    "1","2","3","4","5","6","7","8","9","12","24","36","60"
            },
            optionNames = {
                    "1个月","2个月","3个月","4个月","5个月","6个月","7个月","8个月","9个月","1年","2年","3年","5年"
            },
            conditions = "this.chargeMode === 'prePaid'",
            defaultValues = "1"
    )
    private Long period;

    @BooleanField(label = "自动续费", conditions = "this.chargeMode === 'prePaid'")
    private boolean autoRenew;

    @SelectField(
            label = "数据库引擎",
            options = {
                    "MySQL",
                    "PostgreSQL",
                    "SQLServer"
            },
            optionNames = {
                    "MySQL",
                    "PostgreSQL",
                    "SQLServer"
            },
            defaultValues = "MySQL"
    )
    private DbEngine engine;

    @NestedFormField(label = "MySQL实例信息", nestedFormClass = EngineInput.class, conditions = "this.engine === 'MySQL'")
    private EngineInput mysqlInput;

    @NestedFormField(label = "PostgreSQL实例信息", nestedFormClass = EngineInput.class, conditions = "this.engine === 'PostgreSQL'")
    private EngineInput pgInput;

    @NestedFormField(label = "SQLServer实例信息", nestedFormClass = EngineInput.class, conditions = "this.engine === 'SQLServer'")
    private EngineInput sqlServerInput;

    public static DynamicFormMetaData getFormMetaData(HuaweiCloudClient client){
        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(HuaweiRdsBuildInput.class);

        Future<DynamicFormMetaData> mysqlFuture = RealTimeTaskUtil.submit(
                () -> EngineInput.getFormMetaData(DbEngine.MySQL, client)
        );

        Future<DynamicFormMetaData> pgFuture = RealTimeTaskUtil.submit(
                () -> EngineInput.getFormMetaData(DbEngine.PostgreSQL, client)
        );

        Future<DynamicFormMetaData> sqlServerFuture = RealTimeTaskUtil.submit(
                () -> EngineInput.getFormMetaData(DbEngine.SQLServer, client)
        );

        try {
            formMetaData = DynamicFormHelper.changeNestedFormFieldMetaData(
                    formMetaData,
                    "mysqlInput",
                    mysqlFuture.get()
            );

            formMetaData = DynamicFormHelper.changeNestedFormFieldMetaData(
                    formMetaData,
                    "pgInput",
                    pgFuture.get()
            );

            formMetaData = DynamicFormHelper.changeNestedFormFieldMetaData(
                    formMetaData,
                    "sqlServerInput",
                    sqlServerFuture.get()
            );

            return formMetaData;
        }catch (Exception e){
            throw new StratoException(e.getMessage(), e);
        }
    }

    @JsonIgnore
    public EngineInput getEngineInput(){
        return switch (engine){
            case MySQL -> mysqlInput;
            case PostgreSQL -> pgInput;
            case SQLServer -> sqlServerInput;
            default -> throw new StratoException("Unsupported engine: "+engine);
        };
    }


    @Data
    public static class EngineInput implements DynamicForm {
        @InputField(label = "数据库引擎", conditions = "false")
        private DbEngine engine;
        @SelectField(label = "数据库版本")
        private String engineVersion;
        @SelectField(
                label = "实例类型",
                options = {
                        "Ha",
                        "Single"
                },
                optionNames = {
                        "主备",
                        "单机"
                },
                defaultValues = "Ha"
        )
        private String haMode;

        @SelectField(label = "备机同步模式", conditions = "this.haMode === 'Ha'")
        private String replicationMode;

        @SelectField(label = "备可用区", conditions = "this.haMode === 'Ha'")
        private String backupZone;

        @SelectField(
                label = "规格类型",
                options = {
                        "normal",
                        "dedicated",
                        "armFlavors"
                },
                optionNames = {
                        "通用型",
                        "独享型",
                        "鲲鹏通用增强型"
                },
                defaultValues = "normal"
        )
        private String flavorGroup;

        @SuppressWarnings("SpellCheckingInspection")
        @SelectField(
                label = "规格",
                filterPredicates = {
                        "!formData.haMode || formData.haMode.toLowerCase() === element.haMode",
                        "!formData.engineVersion || (element.engineVersion && element.engineVersion.indexOf(formData.engineVersion) !== -1)",
                        "!formData.flavorGroup || " +
                                "(element.group && formData.flavorGroup === 'normal' && ['normal','normal2','normalLocalssd','general'].indexOf(element.group) !== -1) || " +
                                "(element.group && formData.flavorGroup === 'dedicated' && ['dedicicateNormal','dedicatedNormalLocalssd','dedicated','rapid'].indexOf(element.group) !== -1) || " +
                                "(element.group && formData.flavorGroup === 'armFlavors' && element.group === 'armFlavors')"
                }
        )
        private String flavorCode;

        @SelectField(
                label = "存储类型",
                options = {
                        "CLOUDSSD",
                        "ESSD"
                },
                optionNames = {
                        "SSD云盘",
                        "极速型SSD"
                },
                defaultValues = "CLOUDSSD"
        )
        private String storageType;

        @NumberField(
                label = "存储空间(GB)",
                min = 40,
                max = 4000,
                step = 10,
                defaultValue = 40
        )
        private Integer storageSize;

        @NumberField(
                label = "端口",
                min = 1024,
                max = 65535
        )
        private Integer port;

        @InputField(label = "数据库密码", inputType = "password")
        private String password;

        @BooleanField(label = "表名大小写是否敏感")
        private boolean tableNameCaseSensitive;

        @SelectField(
                label = "参数模板",
                filterPredicates = {
                        "!formData.engineVersion || (element.engineVersion && element.engineVersion === formData.engineVersion)"
                }
        )
        private String configurationId;


        public static DynamicFormMetaData getFormMetaData(DbEngine engine, HuaweiCloudClient client){
            ListFlavorsRequest.DatabaseNameEnum flavorDatabase;
            ListDatastoresRequest.DatabaseNameEnum versionDatabase;

            List<String> replicationModeOptions;
            List<String> replicationModeOptionNames;

            EngineInput engineInput = new EngineInput();

            if(engine == DbEngine.MySQL){
                flavorDatabase = ListFlavorsRequest.DatabaseNameEnum.MYSQL;
                versionDatabase = ListDatastoresRequest.DatabaseNameEnum.MYSQL;

                replicationModeOptions = List.of("async", "semisync");
                replicationModeOptionNames = List.of("异步模式", "半同步模式");
                engineInput.setReplicationMode("semisync");

                engineInput.setPort(3306);
            } else if (engine == DbEngine.PostgreSQL) {
                flavorDatabase = ListFlavorsRequest.DatabaseNameEnum.POSTGRESQL;
                versionDatabase = ListDatastoresRequest.DatabaseNameEnum.POSTGRESQL;

                replicationModeOptions = List.of("async", "sync");
                replicationModeOptionNames = List.of("异步模式", "同步模式");
                engineInput.setReplicationMode("sync");

                engineInput.setPort(5432);
            } else if (engine == DbEngine.SQLServer) {
                flavorDatabase = ListFlavorsRequest.DatabaseNameEnum.SQLSERVER;
                versionDatabase = ListDatastoresRequest.DatabaseNameEnum.SQLSERVER;

                replicationModeOptions = List.of("sync");
                replicationModeOptionNames = List.of("同步模式");
                engineInput.setReplicationMode("sync");

                engineInput.setPort(1433);
            } else {
                throw new StratoException("DB engine %s is not supported".formatted(engine));
            }

            DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(EngineInput.class);

            formMetaData = DynamicFormHelper.changeOptions(
                    formMetaData,
                    "replicationMode",
                    replicationModeOptions,
                    replicationModeOptionNames
            );

            List<NovaAvailabilityZone> zones = client.ecs().describeZones();
            formMetaData = DynamicFormHelper.changeOptions(
                    formMetaData,
                    "backupZone",
                    zones.stream().map(NovaAvailabilityZone::getZoneName).toList(),
                    zones.stream().map(NovaAvailabilityZone::getZoneName).toList()
            );

            ListFlavorsRequest flavorsRequest = new ListFlavorsRequest();
            flavorsRequest.setDatabaseName(flavorDatabase);
            List<Flavor> flavors = client.rds().describeFlavors(flavorsRequest).stream().sorted(
                    Comparator.comparing(Flavor::getVcpus).thenComparingInt(Flavor::getRam)
            ).toList();

            formMetaData = DynamicFormHelper.changeOptions(
                    formMetaData,
                    "flavorCode",
                    flavors.stream().map(Flavor::getSpecCode).toList(),
                    flavors.stream().map(EngineInput::getFlavorName).toList()
            );

            formMetaData = DynamicFormHelper.addProperty(
                    formMetaData,
                    "flavorCode",
                    "haMode",
                    "实例类型",
                    flavors.stream().map(Flavor::getInstanceMode).toList(),
                    false
            );

            formMetaData = DynamicFormHelper.addProperty(
                    formMetaData,
                    "flavorCode",
                    "engineVersion",
                    "数据库版本",
                    flavors.stream().map(Flavor::getVersionName).toList(),
                    false
            );

            formMetaData = DynamicFormHelper.addProperty(
                    formMetaData,
                    "flavorCode",
                    "group",
                    "规格类型",
                    flavors.stream().map(Flavor::getGroupType).toList(),
                    false
            );

            if(engine == DbEngine.MySQL){
                formMetaData = DynamicFormHelper.changeOptions(
                        formMetaData,
                        "engineVersion",
                        List.of("5.7", "8.0"),
                        List.of("5.7", "8.0")
                );
            }else {
                ListDatastoresRequest datastoresRequest = new ListDatastoresRequest();
                datastoresRequest.setDatabaseName(versionDatabase);
                List<LDatastore> datastores = client.rds().describeEngineVersions(datastoresRequest);

                formMetaData = DynamicFormHelper.changeOptions(
                        formMetaData,
                        "engineVersion",
                        datastores.stream().map(LDatastore::getName).toList(),
                        datastores.stream().map(LDatastore::getName).toList()
                );
            }

            List<ConfigurationSummary> configurationSummaries = client.rds().listConfigurations(engine);

            formMetaData = DynamicFormHelper.changeOptions(
                    formMetaData,
                    "configurationId",
                    configurationSummaries.stream().map(ConfigurationSummary::getId).toList(),
                    configurationSummaries.stream().map(ConfigurationSummary::getName).toList()
            );

            formMetaData = DynamicFormHelper.addProperty(
                    formMetaData,
                    "configurationId",
                    "engineVersion",
                    "数据库版本",
                    configurationSummaries.stream().map(ConfigurationSummary::getDatastoreVersionName).toList(),
                    false
            );

            formMetaData = DynamicFormHelper.changeDefaultValues(formMetaData, engineInput);

            return formMetaData;
        }

        public static String getFlavorName(Flavor flavor){
            return "%s %sC%sG %s".formatted(
                    flavor.getSpecCode(),
                    flavor.getVcpus(),
                    flavor.getRam(),
                    getFlavorGroupName(flavor.getGroupType())
            );
        }

        @SuppressWarnings("SpellCheckingInspection")
        public static String getFlavorGroupName(String groupType) {
            if(Utils.isBlank(groupType))
                return "";
            return switch (groupType){
                case "normal" -> "通用增强型";
                case "normal2" -> "通用增强Ⅱ型";
                case "armFlavors" -> "鲲鹏通用增强型";
                case "dedicicateNormal", "dedicatedNormalLocalssd" -> "x86独享型";
                case "armLocalssd" -> "鲲鹏通用型";
                case "normalLocalssd" -> "x86通用型";
                case "general" -> "通用型";
                case "dedicated", "rapid" -> "独享型";
                case "bigmem" -> "超大内存型";
                case "highPerformancePrivilegeEdition" -> "超高IO尊享版";
                default -> groupType;
            };
        }
    }
}
