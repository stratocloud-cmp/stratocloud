package com.stratocloud.provider.aliyun.kafka.actions;

import com.stratocloud.form.BooleanField;
import com.stratocloud.form.NumberField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

import java.util.List;

@Data
public class AliyunKafkaBuildInput implements ResourceActionInput {
    @SelectField(
            label = "付费方式",
            options = {
                    "prepaid",
                    "postpaid"
            },
            optionNames = {
                    "包年包月",
                    "按量计费"
            },
            defaultValues = "postpaid"
    )
    private String payType;

    @SelectField(
            label = "购买时长",
            options = {
                    "1", "2", "4", "5", "6", "12", "24", "36", "48", "60"
            },
            optionNames = {
                    "1个月", "2个月", "3个月", "4个月", "5个月", "6个月",
                    "1年", "2年", "3年", "4年", "5年"
            },
            defaultValues = "1",
            conditions = "this.payType === 'prepaid'"
    )
    private Long period;

    @SelectField(
            label = "规格类型",
            options = {
                    "normal",
                    "professional",
                    "professionalForHighRead"
            },
            optionNames = {
                    "标准版（高写版）",
                    "专业版（高写版）",
                    "专业版（高读版）"
            },
            defaultValues = "normal"
    )
    private String specType;

    @SelectField(
            label = "实例类型",
            options = {
                    "4",
                    "5"
            },
            optionNames = {
                    "公网/VPC实例",
                    "VPC实例"
            },
            defaultValues = "5"
    )
    private Long deployType;

    @NumberField(label = "公网流量(Mbps)", min = 3, max = 160, defaultValue = 3, conditions = "this.deployType === '4'")
    private Integer eipMax;

    @SelectField(
            label = "流量规格",
            options = {
                    "alikafka.hw.2xlarge",
                    "alikafka.hw.3xlarge",
                    "alikafka.hw.6xlarge",
                    "alikafka.hw.9xlarge",
                    "alikafka.hw.12xlarge"
            },
            optionNames = {
                    "alikafka.hw.2xlarge (读峰值:3×20MB/s 写峰值:3×20MB/s 预包含分区数:1000)",
                    "alikafka.hw.3xlarge (读峰值:3×30MB/s 写峰值:3×30MB/s 预包含分区数:1000)",
                    "alikafka.hw.6xlarge (读峰值:3×60MB/s 写峰值:3×60MB/s 预包含分区数:1000)",
                    "alikafka.hw.9xlarge (读峰值:3×90MB/s 写峰值:3×90MB/s 预包含分区数:1000)",
                    "alikafka.hw.12xlarge (读峰值:3×120MB/s 写峰值:3×120MB/s 预包含分区数:1000)"
            },
            defaultValues = "alikafka.hw.2xlarge",
            conditions = "this.specType === 'normal'"
    )
    private String normalIoMaxSpec;

    @SelectField(
            label = "流量规格",
            options = {
                    "alikafka.hw.2xlarge",
                    "alikafka.hw.3xlarge",
                    "alikafka.hw.6xlarge",
                    "alikafka.hw.9xlarge",
                    "alikafka.hw.12xlarge",
                    "alikafka.hw.16xlarge",
                    "alikafka.hw.20xlarge",
                    "alikafka.hw.25xlarge",
                    "alikafka.hw.30xlarge",
                    "alikafka.hw.60xlarge",
                    "alikafka.hw.80xlarge",

                    "alikafka.hw.100xlarge",
                    "alikafka.hw.120xlarge",
                    "alikafka.hw.150xlarge",
                    "alikafka.hw.180xlarge",
                    "alikafka.hw.200xlarge",

                    "alikafka.hw2.220xlarge",
                    "alikafka.hw2.300xlarge",
                    "alikafka.hw2.400xlarge",
                    "alikafka.hw2.500xlarge",
                    "alikafka.hw2.600xlarge",
                    "alikafka.hw2.700xlarge",
                    "alikafka.hw2.800xlarge",
                    "alikafka.hw2.900xlarge",
                    "alikafka.hw2.1000xlarge",
            },
            optionNames = {
                    "alikafka.hw.2xlarge (读峰值:3×20MB/s 写峰值:3×20MB/s 预包含分区数:1000)",
                    "alikafka.hw.3xlarge (读峰值:3×30MB/s 写峰值:3×30MB/s 预包含分区数:1000)",
                    "alikafka.hw.6xlarge (读峰值:3×60MB/s 写峰值:3×60MB/s 预包含分区数:1000)",
                    "alikafka.hw.9xlarge (读峰值:3×90MB/s 写峰值:3×90MB/s 预包含分区数:1000)",
                    "alikafka.hw.12xlarge (读峰值:3×120MB/s 写峰值:3×120MB/s 预包含分区数:1000)",
                    "alikafka.hw.16xlarge (读峰值:3×160MB/s 写峰值:3×160MB/s 预包含分区数:2000)",
                    "alikafka.hw.20xlarge (读峰值:3×200MB/s 写峰值:3×200MB/s 预包含分区数:2000)",
                    "alikafka.hw.25xlarge (读峰值:3×250MB/s 写峰值:3×250MB/s 预包含分区数:2000)",
                    "alikafka.hw.30xlarge (读峰值:3×300MB/s 写峰值:3×300MB/s 预包含分区数:2000)",
                    "alikafka.hw.60xlarge (读峰值:3×600MB/s 写峰值:3×600MB/s 预包含分区数:2000)",
                    "alikafka.hw.80xlarge (读峰值:3×800MB/s 写峰值:3×800MB/s 预包含分区数:2000)",

                    "alikafka.hw.100xlarge (读峰值:3×1000MB/s 写峰值:3×1000MB/s 预包含分区数:3000)",
                    "alikafka.hw.120xlarge (读峰值:3×1200MB/s 写峰值:3×1200MB/s 预包含分区数:3000)",
                    "alikafka.hw.150xlarge (读峰值:3×1500MB/s 写峰值:3×1500MB/s 预包含分区数:3000)",
                    "alikafka.hw.180xlarge (读峰值:3×1800MB/s 写峰值:3×1800MB/s 预包含分区数:3000)",
                    "alikafka.hw.200xlarge (读峰值:3×2000MB/s 写峰值:3×2000MB/s 预包含分区数:3000)",

                    "alikafka.hw2.220xlarge (读峰值:3×2200MB/s 写峰值:3×2200MB/s 预包含分区数:4000)",
                    "alikafka.hw2.300xlarge (读峰值:3×3000MB/s 写峰值:3×3000MB/s 预包含分区数:4000)",
                    "alikafka.hw2.400xlarge (读峰值:3×4000MB/s 写峰值:3×4000MB/s 预包含分区数:4000)",
                    "alikafka.hw2.500xlarge (读峰值:3×5000MB/s 写峰值:3×5000MB/s 预包含分区数:4000)",
                    "alikafka.hw2.600xlarge (读峰值:3×6000MB/s 写峰值:3×6000MB/s 预包含分区数:5000)",
                    "alikafka.hw2.700xlarge (读峰值:3×7000MB/s 写峰值:3×7000MB/s 预包含分区数:5000)",
                    "alikafka.hw2.800xlarge (读峰值:3×8000MB/s 写峰值:3×8000MB/s 预包含分区数:5000)",
                    "alikafka.hw2.900xlarge (读峰值:3×9000MB/s 写峰值:3×9000MB/s 预包含分区数:5000)",
                    "alikafka.hw2.1000xlarge (读峰值:3×10000MB/s 写峰值:3×10000MB/s 预包含分区数:5000)",
            },
            conditions = "this.specType === 'professional'",
            defaultValues = "alikafka.hw.2xlarge"
    )
    private String proIoMaxSpec;

    @SelectField(
            label = "流量规格",
            options = {
                    "alikafka.hr.2xlarge",
                    "alikafka.hr.3xlarge",
                    "alikafka.hr.6xlarge",
                    "alikafka.hr.9xlarge",
                    "alikafka.hr.12xlarge",
                    "alikafka.hr.16xlarge",
                    "alikafka.hr.20xlarge",
                    "alikafka.hr.25xlarge",
                    "alikafka.hr.30xlarge",
                    "alikafka.hr.60xlarge",
                    "alikafka.hr.80xlarge",

                    "alikafka.hr.100xlarge",
                    "alikafka.hr.120xlarge",
                    "alikafka.hr.150xlarge",
                    "alikafka.hr.180xlarge",
                    "alikafka.hr.200xlarge",

                    "alikafka.hr2.220xlarge",
                    "alikafka.hr2.300xlarge",
                    "alikafka.hr2.400xlarge",
                    "alikafka.hr2.500xlarge",
                    "alikafka.hr2.600xlarge",
                    "alikafka.hr2.700xlarge",
                    "alikafka.hr2.800xlarge",
                    "alikafka.hr2.900xlarge",
                    "alikafka.hr2.1000xlarge",
            },
            optionNames = {
                    "alikafka.hr.2xlarge (读峰值:50+2×10MB/s 写峰值:10+2×10MB/s 预包含分区数:1000)",
                    "alikafka.hr.3xlarge (读峰值:75+2×15MB/s 写峰值:15+2×15MB/s 预包含分区数:1000)",
                    "alikafka.hr.6xlarge (读峰值:150+2×30MB/s 写峰值:30+2×30MB/s 预包含分区数:1000)",
                    "alikafka.hr.9xlarge (读峰值:180+2×45MB/s 写峰值:45+2×45MB/s 预包含分区数:1000)",
                    "alikafka.hr.12xlarge (读峰值:240+2×60MB/s 写峰值:60+2×60MB/s 预包含分区数:1000)",
                    "alikafka.hr.16xlarge (读峰值:240+2×80MB/s 写峰值:80+2×80MB/s 预包含分区数:2000)",
                    "alikafka.hr.20xlarge (读峰值:300+2×100MB/s 写峰值:100+2×100MB/s 预包含分区数:2000)",
                    "alikafka.hr.25xlarge (读峰值:375+2×125MB/s 写峰值:125+2×125MB/s 预包含分区数:2000)",
                    "alikafka.hr.30xlarge (读峰值:450+2×150MB/s 写峰值:150+2×150MB/s 预包含分区数:2000)",
                    "alikafka.hr.60xlarge (读峰值:900+2×300MB/s 写峰值:300+2×300MB/s 预包含分区数:2000)",
                    "alikafka.hr.80xlarge (读峰值:1200+2×400MB/s 写峰值:400+2×400MB/s 预包含分区数:2000)",

                    "alikafka.hr.100xlarge (读峰值:1500+2×500MB/s 写峰值:500+2×500MB/s 预包含分区数:3000)",
                    "alikafka.hr.120xlarge (读峰值:1800+2×600MB/s 写峰值:600+2×600MB/s 预包含分区数:3000)",
                    "alikafka.hr.150xlarge (读峰值:2250+2×750MB/s 写峰值:750+2×750MB/s 预包含分区数:3000)",
                    "alikafka.hr.180xlarge (读峰值:2700+2×900MB/s 写峰值:900+2×900MB/s 预包含分区数:3000)",
                    "alikafka.hr.200xlarge (读峰值:3000+2×1000MB/s 写峰值:1000+2×1000MB/s 预包含分区数:3000)",

                    "alikafka.hr2.220xlarge (读峰值:3300+2×1100MB/s 写峰值:1100+2×1100MB/s 预包含分区数:4000)",
                    "alikafka.hr2.300xlarge (读峰值:4500+2×1500MB/s 写峰值:1500+2×1500MB/s 预包含分区数:4000)",
                    "alikafka.hr2.400xlarge (读峰值:6000+2×2000MB/s 写峰值:2000+2×2000MB/s 预包含分区数:4000)",
                    "alikafka.hr2.500xlarge (读峰值:7500+2×2500MB/s 写峰值:2500+2×2500MB/s 预包含分区数:4000)",
                    "alikafka.hr2.600xlarge (读峰值:9000+2×3000MB/s 写峰值:3000+2×3000MB/s 预包含分区数:5000)",
                    "alikafka.hr2.700xlarge (读峰值:10500+2×3500MB/s 写峰值:3500+2×3500MB/s 预包含分区数:5000)",
                    "alikafka.hr2.800xlarge (读峰值:12000+2×4000MB/s 写峰值:4000+2×4000MB/s 预包含分区数:5000)",
                    "alikafka.hr2.900xlarge (读峰值:13500+2×4500MB/s 写峰值:4500+2×4500MB/s 预包含分区数:5000)",
                    "alikafka.hr2.1000xlarge (读峰值:15000+2×5000MB/s 写峰值:5000+2×5000MB/s 预包含分区数:5000)",
            },
            conditions = "this.specType === 'professional'",
            defaultValues = "alikafka.hr.2xlarge"
    )
    private String hrIoMaxSpec;

    @NumberField(label = "额外购买分区数", defaultValue = 0)
    private Integer partitionNum;

    @SelectField(
            label = "磁盘类型",
            options = {
                    "0",
                    "1"
            },
            optionNames = {
                    "高效云盘",
                    "SSD"
            },
            defaultValues = "0"
    )
    private String diskType;

    @NumberField(
            label = "磁盘容量(GB)",
            min = 100,
            step = 100,
            defaultValue = 600,
            description = """
                    数据默认 3 副本存储。
                    实例规格为标准版时，如购买 300G 磁盘，实际存储业务的磁盘大小为 100G，其余 200G 为备份容量。
                    实例规格为专业版时，如购买 300G 磁盘，实际存储业务的磁盘大小为 300G，额外赠送 600G 备份容量。
                    """
    )
    private Integer diskSize;

    @BooleanField(label = "跨可用区部署", defaultValue = true)
    private boolean crossZone;

    @SelectField(label = "备可用区", conditions = "this.crossZone === true", multiSelect = true)
    private List<String> backupZones;


}
