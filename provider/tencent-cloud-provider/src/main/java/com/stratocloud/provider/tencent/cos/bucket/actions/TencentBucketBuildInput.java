package com.stratocloud.provider.tencent.cos.bucket.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.qcloud.cos.model.CannedAccessControlList;
import com.stratocloud.form.BooleanField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.tencent.cos.bucket.TencentBucketSpec;
import lombok.Data;

@Data
public class TencentBucketBuildInput implements ResourceActionInput {
    @SelectField(
            label = "预设访问权限",
            options = {
                    "Private",
                    "PublicRead",
                    "PublicReadWrite"
            },
            optionNames = {
                    "私有读写",
                    "公有读私有写 (高风险)",
                    "公有读写 (高风险)"
            },
            defaultValues = "Private"
    )
    private CannedAccessControlList aclType;

    @BooleanField(
            label = "多AZ特性",
            description = """
                    多AZ特性允许用户将数据存储在同地理区域内的不同物理位置，提供同城容灾功能，推荐开启。
                    多AZ配置会导致存储容量费用相比单AZ有增加，详情请参考该地域的产品价格。
                    多AZ特性开启后无法关闭 ，数据将存储为多AZ类型。
                    若关闭，将存储为单AZ类型，请根据业务需求谨慎选择，避免后续产生迁移成本。多AZ和单AZ存储的对比请参见腾讯云文档。
                    https://cloud.tencent.com/document/product/436/40548
                    """
    )
    private boolean enableMultiAz;

    @BooleanField(
            label = "版本控制",
            description = """
                    开启版本控制后可以恢复因覆盖或误删丢失的数据。
                    在相同存储桶中保留对象的多个版本，将产生存储容量费用。
                    https://cloud.tencent.com/document/product/436/19881
                    """
    )
    private boolean enableVersioning;

    @BooleanField(
            label = "智能分层"
    )
    private boolean enableIntelligentTier;
    @SelectField(
            label = "低频层转换天数",
            options = {
                    "30", "60", "90"
            },
            optionNames = {
                    "30", "60", "90"
            },
            conditions = "this.enableIntelligentTier === true"
    )
    private Integer defaultIntelligentTierDays = 30;


    @JsonIgnore
    public TencentBucketSpec toSpec(){
        TencentBucketSpec bucketSpec = new TencentBucketSpec();
        bucketSpec.setAclType(aclType);
        bucketSpec.setEnableMultiAz(enableMultiAz);
        bucketSpec.setEnableVersioning(enableVersioning);

        bucketSpec.setEnableIntelligentTier(enableIntelligentTier);
        bucketSpec.setDefaultIntelligentTierDays(defaultIntelligentTierDays);

        return bucketSpec;
    }
}
