package com.stratocloud.provider.aliyun.rds.actions;

import com.aliyun.vpc20160428.models.DescribeVSwitchesRequest;
import com.aliyun.vpc20160428.models.DescribeVpcsRequest;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.form.*;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.form.info.NumberFieldDetail;
import com.stratocloud.provider.aliyun.common.AliyunClient;
import com.stratocloud.provider.aliyun.rds.model.RdsInstanceClass;
import com.stratocloud.provider.aliyun.subnet.AliyunSubnet;
import com.stratocloud.provider.aliyun.vpc.AliyunVpc;
import com.stratocloud.provider.aliyun.zone.AliyunZone;
import com.stratocloud.provider.constants.DbEngine;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class AliyunRdsBuildInput implements ResourceActionInput {
    @SelectField(
            label = "计费模式",
            options = {
                    "Postpaid",
                    "Prepaid"
            },
            optionNames = {
                    "按量计费",
                    "包年包月"
            },
            defaultValues = "Postpaid"
    )
    private String payType;

    @SelectField(
            label = "购买时长",
            options = {
                    "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "24", "36", "48", "60"
            },
            optionNames = {
                    "1个月", "2个月", "3个月", "4个月", "5个月", "6个月", "7个月", "8个月", "9个月", "10个月", "11个月",
                    "1年", "2年", "3年", "4年", "5年"
            },
            conditions = "this.payType === 'Prepaid'",
            defaultValues = "1"
    )
    private Long period;

    @SelectField(
            label = "自动续费",
            options = {
                    "false",
                    "true"
            },
            optionNames = {
                    "否",
                    "是"
            },
            defaultValues = "false",
            conditions = "this.payType === 'Prepaid'"
    )
    private String autoRenew;
    @BooleanField(label = "自动使用代金券")
    private boolean autoUseCoupon;

    @SelectField(
            label = "数据库引擎",
            options = {
                    "MySQL",
                    "PostgreSQL",
                    "SQLServer",
                    "MariaDB"
            },
            optionNames = {
                    "MySQL",
                    "PostgreSQL",
                    "SQLServer",
                    "MariaDB"
            },
            defaultValues = "MySQL"
    )
    private DbEngine engine;

    @NestedFormField(
            label = "MySQL实例参数",
            nestedFormClass = EngineSpecificInput.class,
            conditions = "this.engine === 'MySQL'"
    )
    private EngineSpecificInput mysqlInput;
    @NestedFormField(
            label = "PostgreSQL实例参数",
            nestedFormClass = EngineSpecificInput.class,
            conditions = "this.engine === 'PostgreSQL'"
    )
    private EngineSpecificInput pgsqlInput;
    @NestedFormField(
            label = "SQLServer实例参数",
            nestedFormClass = EngineSpecificInput.class,
            conditions = "this.engine === 'SQLServer'"
    )
    private EngineSpecificInput sqlServerInput;
    @NestedFormField(
            label = "MariaDB实例参数",
            nestedFormClass = EngineSpecificInput.class,
            conditions = "this.engine === 'MariaDB'"
    )
    private EngineSpecificInput mariadbInput;


    @BooleanField(label = "自动创建数据库代理")
    private boolean autoCreateProxy;

    public static DynamicFormMetaData getFormMeta(AliyunClient client){
        List<AliyunVpc> vpcs = client.vpc().describeVpcs(new DescribeVpcsRequest());
        List<AliyunZone> zones = client.ecs().describeZones();
        List<AliyunSubnet> subnets = client.vpc().describeSubnets(new DescribeVSwitchesRequest());
        List<RdsInstanceClass> instanceClasses = client.rds().describeInstanceClasses();

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(AliyunRdsBuildInput.class);

        formMetaData = DynamicFormHelper.changeNestedFormFieldMetaData(
                formMetaData,
                "mysqlInput",
                EngineSpecificInput.getFormMeta(
                        DbEngine.MySQL,
                        vpcs,
                        zones,
                        subnets,
                        instanceClasses
                )
        );

        formMetaData = DynamicFormHelper.changeNestedFormFieldMetaData(
                formMetaData,
                "pgsqlInput",
                EngineSpecificInput.getFormMeta(
                        DbEngine.PostgreSQL,
                        vpcs,
                        zones,
                        subnets,
                        instanceClasses
                )
        );

        formMetaData = DynamicFormHelper.changeNestedFormFieldMetaData(
                formMetaData,
                "sqlServerInput",
                EngineSpecificInput.getFormMeta(
                        DbEngine.SQLServer,
                        vpcs,
                        zones,
                        subnets,
                        instanceClasses
                )
        );

        formMetaData = DynamicFormHelper.changeNestedFormFieldMetaData(
                formMetaData,
                "mariadbInput",
                EngineSpecificInput.getFormMeta(
                        DbEngine.MariaDB,
                        vpcs,
                        zones,
                        subnets,
                        instanceClasses
                )
        );

        return formMetaData;
    }

    public EngineSpecificInput getEngineSpecificInput(){
        return switch (engine){
            case MySQL -> mysqlInput;
            case PostgreSQL -> pgsqlInput;
            case SQLServer -> sqlServerInput;
            case MariaDB -> mariadbInput;
        };
    }

    @Data
    public static class EngineSpecificInput implements DynamicForm {
        @SelectField(label = "数据库引擎", conditions = "false")
        private DbEngine engine;
        @SelectField(label = "版本")
        private String engineVersion;
        @SelectField(label = "实例系列")
        private RdsInstanceClass.Category category;
        @SelectField(label = "实例规格", conditions = "this.category === 'Basic'")
        private String classCodeBasic;
        @SelectField(label = "实例规格", conditions = "this.category === 'HighAvailability'")
        private String classCodeHa;
        @SelectField(label = "实例规格", conditions = "this.category === 'cluster' || this.category === 'AlwaysOn'")
        private String classCodeCluster;

        @BooleanField(
                label = "多可用区部署",
                conditions = "this.category === 'HighAvailability' || this.category === 'cluster' || this.category === 'AlwaysOn'"
        )
        private boolean enableMultiZone;

        @NumberField(
                label = "备节点数量",
                min = 1,
                max = 2,
                conditions = {
                        "this.enableMultiZone === true",
                        "this.category === 'cluster'"
                },
                defaultValue = 1
        )
        private Long slaveNumber;
        @SelectField(
                label = "备可用区及网络",
                conditions = {
                        "this.enableMultiZone === true",
                        "this.category === 'HighAvailability' || this.category === 'cluster'"
                }
        )
        private String vSwitchIdSlave1;
        @SelectField(
                label = "备可用区及网络",
                conditions = {
                        "this.enableMultiZone === true",
                        "this.category === 'cluster' && this.slaveNumber > 1"
                }
        )
        private String vSwitchIdSlave2;
        @SelectField(
                label = "存储类型",
                options = {
                        "local_ssd",
                        "general_essd",
                        "cloud_essd",
                        "cloud_essd2",
                        "cloud_essd3"
                },
                optionNames = {
                        "高性能本地盘",
                        "高性能云盘",
                        "ESSD PL1",
                        "ESSD PL2",
                        "ESSD PL3"
                },
                defaultValues = "general_essd"
        )
        private String storageType;
        @NumberField(label = "存储空间(GB)", min = 10, step = 5, defaultValue = 100)
        private Integer storageSize;

        @NumberField(label = "端口")
        private Integer port;
        @BooleanField(label = "表名区分大小写")
        private boolean ignoreCase;
        @BooleanField(label = "将VPC网段加入到RDS实例白名单", defaultValue = true)
        private boolean grantVpcAccess;

        @JsonIgnore
        public String getClassCode() {
            return switch (category) {
                case Basic -> classCodeBasic;
                case HighAvailability -> classCodeHa;
                case cluster, AlwaysOn -> classCodeCluster;
                default -> throw new StratoException("Unsupported category: " + category);
            };
        }

        public static DynamicFormMetaData getFormMeta(DbEngine engine,
                                                      List<AliyunVpc> vpcs,
                                                      List<AliyunZone> zones,
                                                      List<AliyunSubnet> subnets,
                                                      List<RdsInstanceClass> instanceClasses){
            EngineSpecificInput input = new EngineSpecificInput();
            input.setEngine(engine);
            input.setGrantVpcAccess(true);

            DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(EngineSpecificInput.class);

            Map<String, String> vpcNameMap = vpcs.stream().collect(
                    Collectors.toMap(
                            AliyunVpc::getVpcId,
                            vpc -> vpc.detail().getVpcName()
                    )
            );

            Map<String, String> zoneMap = zones.stream().collect(
                    Collectors.toMap(
                            AliyunZone::getZoneId,
                            zone -> zone.zone().getLocalName()
                    )
            );

            List<String> subnetIds = subnets.stream().map(s -> s.detail().getVSwitchId()).toList();
            List<String> subnetNames = subnets.stream().map(
                    s -> "可用区:%s VPC:%s 子网:%s 网段:%s".formatted(
                            zoneMap.getOrDefault(s.detail().getZoneId(), s.detail().getZoneId()),
                            vpcNameMap.getOrDefault(s.detail().getVpcId(), s.detail().getVpcId()),
                            s.detail().getVSwitchName(),
                            s.detail().getCidrBlock()
                    )
            ).toList();

            formMetaData = DynamicFormHelper.changeOptions(
                    formMetaData,
                    "vSwitchIdSlave1",
                    subnetIds,
                    subnetNames
            );

            formMetaData = DynamicFormHelper.changeOptions(
                    formMetaData,
                    "vSwitchIdSlave2",
                    subnetIds,
                    subnetNames
            );

            Comparator<RdsInstanceClass> comparator = Comparator.comparing(
                    RdsInstanceClass::getGroup
            );
            comparator = comparator.thenComparing(
                    RdsInstanceClass::getMaxConnections
            );

            List<RdsInstanceClass> engineClasses = instanceClasses.stream().filter(
                    c -> c.engine() == engine
            ).sorted(comparator).toList();

            List<RdsInstanceClass> basicClasses = engineClasses.stream().filter(
                    c -> Set.of(
                            RdsInstanceClass.Category.Basic,
                            RdsInstanceClass.Category.Empty
                    ).contains(c.getCategory())
            ).toList();

            List<RdsInstanceClass> haClasses = engineClasses.stream().filter(
                    c -> Set.of(
                            RdsInstanceClass.Category.HighAvailability,
                            RdsInstanceClass.Category.Empty
                    ).contains(c.getCategory())
            ).toList();

            List<RdsInstanceClass> clusterClasses = engineClasses.stream().filter(
                    c -> Set.of(
                            RdsInstanceClass.Category.cluster,
                            RdsInstanceClass.Category.AlwaysOn,
                            RdsInstanceClass.Category.Empty
                    ).contains(c.getCategory())
            ).toList();

            formMetaData = DynamicFormHelper.changeOptions(
                    formMetaData,
                    "classCodeBasic",
                    basicClasses.stream().map(c -> c.detail().getClassCode()).toList(),
                    basicClasses.stream().map(RdsInstanceClass::getName).toList()
            );

            formMetaData = DynamicFormHelper.changeOptions(
                    formMetaData,
                    "classCodeHa",
                    haClasses.stream().map(c -> c.detail().getClassCode()).toList(),
                    haClasses.stream().map(RdsInstanceClass::getName).toList()
            );

            formMetaData = DynamicFormHelper.changeOptions(
                    formMetaData,
                    "classCodeCluster",
                    clusterClasses.stream().map(c -> c.detail().getClassCode()).toList(),
                    clusterClasses.stream().map(RdsInstanceClass::getName).toList()
            );

            List<String> engineVersions;
            List<RdsInstanceClass.Category> categories;

            if(engine == DbEngine.MySQL){
                engineVersions = List.of("5.5", "5.6", "5.7", "8.0");

                input.setEngineVersion("8.0");

                input.setCategory(RdsInstanceClass.Category.HighAvailability);

                categories = List.of(
                        RdsInstanceClass.Category.Basic,
                        RdsInstanceClass.Category.HighAvailability,
                        RdsInstanceClass.Category.cluster
                );

                formMetaData = DynamicFormHelper.changeFieldDetail(
                        formMetaData,
                        "port",
                        new NumberFieldDetail(
                                3306,
                                1000,
                                65534,
                                1,
                                true,
                                List.of(),
                                ""
                        )
                );
            }else if(engine == DbEngine.PostgreSQL){
                engineVersions = List.of(
                        "10.0","11.0","12.0","13.0","14.0","15.0","16.0","17.0"
                );

                input.setEngineVersion("17.0");

                input.setCategory(RdsInstanceClass.Category.HighAvailability);

                categories = List.of(
                        RdsInstanceClass.Category.Basic,
                        RdsInstanceClass.Category.HighAvailability,
                        RdsInstanceClass.Category.cluster
                );

                formMetaData = DynamicFormHelper.changeFieldDetail(
                        formMetaData,
                        "port",
                        new NumberFieldDetail(
                                5432,
                                1000,
                                5999,
                                1,
                                true,
                                List.of(),
                                ""
                        )
                );
            }else if(engine == DbEngine.SQLServer){
                engineVersions = List.of(
                        "2012", "2012_ent_ha", "2012_std_ha", "2012_web", "2014_ent_ha", "2014_std_ha",
                        "2016_ent_ha", "2016_std_ha", "2016_web", "2017_ent", "2017_std_ha", "2017_web", "2019_ent",
                        "2019_std_ha", "2019_web", "2022_ent", "2022_std_ha", "2022_web"
                );

                input.setEngineVersion("2022_std_ha");

                input.setCategory(RdsInstanceClass.Category.HighAvailability);

                categories = List.of(
                        RdsInstanceClass.Category.Basic,
                        RdsInstanceClass.Category.HighAvailability,
                        RdsInstanceClass.Category.AlwaysOn
                );

                formMetaData = DynamicFormHelper.changeFieldDetail(
                        formMetaData,
                        "port",
                        new NumberFieldDetail(
                                1433,
                                1000,
                                5999,
                                1,
                                true,
                                List.of(),
                                ""
                        )
                );
            }else if(engine == DbEngine.MariaDB){
                engineVersions = List.of("10.3","10.6");

                input.setEngineVersion("10.6");

                input.setCategory(RdsInstanceClass.Category.HighAvailability);

                categories = List.of(RdsInstanceClass.Category.HighAvailability);

                formMetaData = DynamicFormHelper.changeFieldDetail(
                        formMetaData,
                        "port",
                        new NumberFieldDetail(
                                3306,
                                1000,
                                5999,
                                1,
                                true,
                                List.of(),
                                ""
                        )
                );
            }else {
                throw new StratoException("Unsupported rds engine: " + engine);
            }

            formMetaData = DynamicFormHelper.changeDefaultValues(formMetaData, input);

            formMetaData = DynamicFormHelper.changeOptions(
                    formMetaData,
                    "engineVersion",
                    engineVersions,
                    engineVersions
            );

            formMetaData = DynamicFormHelper.changeOptions(
                    formMetaData,
                    "category",
                    categories.stream().map(RdsInstanceClass.Category::name).toList(),
                    categories.stream().map(RdsInstanceClass.Category::getDisplayName).toList()
            );

            return formMetaData;
        }
    }
}
