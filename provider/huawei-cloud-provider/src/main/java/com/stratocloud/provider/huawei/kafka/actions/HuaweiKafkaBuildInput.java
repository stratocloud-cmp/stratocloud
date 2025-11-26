package com.stratocloud.provider.huawei.kafka.actions;

import com.huaweicloud.sdk.kafka.v2.model.AvailableZonesResp;
import com.huaweicloud.sdk.kafka.v2.model.ListEngineProductsEntity;
import com.stratocloud.form.BooleanField;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.NumberField;
import com.stratocloud.form.SelectField;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.huawei.common.HuaweiCloudClient;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.utils.Utils;
import lombok.Data;

import java.util.List;

@Data
public class HuaweiKafkaBuildInput implements ResourceActionInput {
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
            label = "Kafka版本",
            options = {
                    "1.1.0", "2.3.0", "2.7", "3.x"
            },
            optionNames = {
                    "1.1.0", "2.3.0", "2.7", "3.x"
            },
            defaultValues = "3.x"
    )
    private String engineVersion;

    @SelectField(
            label = "部署架构",
            options = {
                    "single",
                    "cluster"
            },
            optionNames = {
                    "单机",
                    "集群"
            },
            defaultValues = "cluster"

    )
    private String instanceType;

    @BooleanField(label = "使用备可用区", conditions = "this.instanceType === 'cluster'")
    private boolean enableBackupZones;

    @SelectField(label = "备可用区1", conditions = "this.instanceType === 'cluster' && this.enableBackupZones === true")
    private String firstBackupZone;

    @SelectField(label = "备可用区2", conditions = "this.instanceType === 'cluster' && this.enableBackupZones === true")
    private String secondBackupZone;

    @SelectField(
            label = "实例规格",
            filterPredicates = "!formData.instanceType || element.type === formData.instanceType"
    )
    private String productId;

    @NumberField(label = "代理数量", min = 3, max = 50, defaultValue = 3)
    private Integer brokerNumber;

    @SelectField(
            label = "云硬盘类型",
            options = {
                    "dms.physical.storage.high.v2",
                    "dms.physical.storage.ultra.v2",
                    "dms.physical.storage.general",
                    "dms.physical.storage.extreme"
            },
            optionNames = {
                    "高IO云硬盘",
                    "超高IO云硬盘",
                    "通用型SSD云硬盘",
                    "极速型SSD云硬盘"
            },
            defaultValues = "dms.physical.storage.extreme"
    )
    private String storageSpecCode;

    @NumberField(label = "单个代理存储空间(GB)", min = 100, defaultValue = 100, step = 100)
    private Integer storageSpace;

    @SelectField(
            label = "容量阈值策略",
            options = {
                    "time_base",
                    "produce_reject"
            },
            optionNames = {
                    "自动删除",
                    "生产受限"
            },
            defaultValues = "time_base"
    )
    private String retentionPolicy;

    @BooleanField(label = "自动创建Topic")
    private boolean enableAutoTopic;


    public static DynamicFormMetaData getFormMeta(HuaweiCloudClient client){
        List<AvailableZonesResp> zones = client.kafka().describeZones();
        List<ListEngineProductsEntity> products = client.kafka().describeProducts().stream().filter(
                p -> Utils.length(p.getChargingMode()) == 2
        ).toList();

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(HuaweiKafkaBuildInput.class);

        formMetaData = DynamicFormHelper.changeOptions(
                formMetaData,
                "firstBackupZone",
                zones.stream().map(AvailableZonesResp::getCode).toList(),
                zones.stream().map(AvailableZonesResp::getName).toList()
        );

        formMetaData = DynamicFormHelper.changeOptions(
                formMetaData,
                "secondBackupZone",
                zones.stream().map(AvailableZonesResp::getCode).toList(),
                zones.stream().map(AvailableZonesResp::getName).toList()
        );

        List<String> productIds = products.stream().map(ListEngineProductsEntity::getProductId).toList();
        List<String> productTypes = products.stream().map(ListEngineProductsEntity::getType).toList();

        formMetaData = DynamicFormHelper.changeOptions(
                formMetaData,
                "productId",
                productIds,
                productIds
        );

        formMetaData = DynamicFormHelper.addProperty(
                formMetaData,
                "productId",
                "type",
                "实例类型",
                productTypes,
                false
        );

        return formMetaData;
    }
}
