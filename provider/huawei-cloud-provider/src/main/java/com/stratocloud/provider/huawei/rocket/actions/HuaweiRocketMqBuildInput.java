package com.stratocloud.provider.huawei.rocket.actions;

import com.huaweicloud.sdk.rocketmq.v2.model.ListAvailableZonesRespAvailableZones;
import com.huaweicloud.sdk.rocketmq.v2.model.ProductEntity;
import com.stratocloud.form.BooleanField;
import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.form.NumberField;
import com.stratocloud.form.SelectField;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.provider.huawei.common.HuaweiCloudClient;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

import java.util.List;

@Data
public class HuaweiRocketMqBuildInput implements ResourceActionInput {
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
            label = "部署架构",
            options = {
                    "cluster",
                    "single.basic",
                    "cluster.basic",
                    "cluster.professional"
            },
            optionNames = {
                    "4.8.0集群",
                    "5.x单机基础版",
                    "5.x集群基础版",
                    "5.x集群专业版"
            },
            defaultValues = "cluster.basic"
    )
    private String instanceType;

    @SelectField(label = "备可用区", conditions = "this.instanceType !== 'single.basic'", required = false)
    private String backupZone;


    @SelectField(
            label = "芯片架构",
            options = {
                    "X86"
            },
            optionNames = {
                    "X86"
            },
            defaultValues = "X86"
    )
    private String archType;

    @SelectField(
            label = "实例规格",
            filterPredicates = {
                    "!formData.instanceType || element.type === formData.instanceType",
                    "!formData.archType || (element.archTypes && element.archTypes.indexOf(formData.archType) !== -1)"
            }
    )
    private String productId;

    @NumberField(label = "代理数量", min = 3, max = 50, defaultValue = 3, conditions = "this.instanceType === 'cluster'")
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


    public static DynamicFormMetaData getFormMeta(HuaweiCloudClient client){
        var zones = client.rocket().describeZones();
        var products = client.rocket().describeProducts();

        DynamicFormMetaData formMetaData = DynamicFormHelper.generateMetaData(HuaweiRocketMqBuildInput.class);

        formMetaData = DynamicFormHelper.changeOptions(
                formMetaData,
                "backupZone",
                zones.stream().map(ListAvailableZonesRespAvailableZones::getCode).toList(),
                zones.stream().map(ListAvailableZonesRespAvailableZones::getCode).toList()
        );

        List<String> productIds = products.stream().map(ProductEntity::getProductId).toList();
        List<String> productTypes = products.stream().map(ProductEntity::getType).toList();
        List<List<String>> productArchTypes = products.stream().map(ProductEntity::getArchTypes).toList();

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

        formMetaData = DynamicFormHelper.addProperty(
                formMetaData,
                 "productId",
                "archTypes",
                "芯片架构",
                productArchTypes,
                false
        );

        return formMetaData;
    }
}
