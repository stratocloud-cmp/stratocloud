package com.stratocloud.provider.tencent.rocketmq.actions;

import com.stratocloud.form.BooleanField;
import com.stratocloud.form.NumberField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class TencentRocketBuildInput implements ResourceActionInput {
    /**
     * 付费模式（0: 后付费；1: 预付费），默认值为0
     */
    @SelectField(
            label = "计费模式",
            options = {
                    "0",
                    "1"
            },
            optionNames = {
                    "按量计费",
                    "包年包月"
            },
            defaultValues = "0"
    )
    private Long payMode;

    /**
     * 预付费集群是否自动续费（0: 不自动续费；1: 自动续费），默认值为0
     */
    @SelectField(
            label = "是否自动续费",
            options = {
                    "0",
                    "1"
            },
            optionNames = {
                    "否",
                    "是"
            },
            defaultValues = "0",
            conditions = "this.payMode === '1'"
    )
    private Long renewFlag;

    /**
     * 预付费集群的购买时长（单位：月），取值范围为1～60，默认值为1
     */
    @SelectField(
            label = "购买时长",
            options = {
                    "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "24", "36", "48", "60"
            },
            optionNames = {
                    "1个月", "2个月", "3个月", "4个月", "5个月", "6个月", "7个月", "8个月", "9个月", "10个月", "11个月",
                    "1年", "2年", "3年", "4年", "5年"
            },
            conditions = "this.payMode === '1'",
            defaultValues = "1"
    )
    private Long timeSpan;

    @SelectField(label = "集群规格")
    private String skuCode;

    /**
     * 消息保留时长（单位：小时），取值范围参考 [DescribeProductSKUs](https://cloud.tencent.com/document/api/1493/107676) 接口中的 [ProductSKU](https://cloud.tencent.com/document/api/1493/96031#ProductSKU) 出参：

     - 默认值：DefaultRetention 参数
     - 最小值：RetentionLowerLimit 参数
     - 最大值：RetentionUpperLimit 参数
     */
    @NumberField(
            label = "消息保留时长(小时)",
            min = 24,
            max = 72,
            defaultValue = 72,
            conditions = "this.skuCode && this.skuCode.indexOf('basic') !== -1"
    )
    private Long messageRetention;


    /**
     * 最大可创建主题数，从 [DescribeProductSKUs](https://cloud.tencent.com/document/api/1493/107676) 接口中的 [ProductSKU](https://cloud.tencent.com/document/api/1493/96031#ProductSKU) 出参：

     - 默认值和最小值：TopicNumLimit 参数
     - 最大值：TopicNumUpperLimit 参数
     */
    @NumberField(
            label = "额外购买Topic个数",
            defaultValue = 0
    )
    private Long extraMaxTopicNum;

    @BooleanField(label = "多可用区部署")
    private boolean enableMultiZone;
    @SelectField(label = "备可用区", conditions = "this.enableMultiZone === true")
    private String backupZoneId;


    /**
     * 是否开启公网，默认值为false表示不开启
     */
    @BooleanField(label = "公网访问")
    private boolean enablePublic;

    /**
     * 公网是否按流量计费，默认值为false表示不按流量计费
     */
    @BooleanField(label = "公网是否按流量计费", conditions = "this.enablePublic === true")
    private boolean billingFlow;

    /**
     * 公网带宽（单位：兆），默认值为0。如果开启公网，该字段必须为大于0的正整数
     */
    @NumberField(label = "公网带宽(Mbps)", min = 1, max = 1024, defaultValue = 1, conditions = "this.enablePublic === true")
    private Long bandwidth;
}
