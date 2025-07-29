package com.stratocloud.provider.tencent.cos.bucket.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.stratocloud.form.BooleanField;
import com.stratocloud.form.InputField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.resource.ResourceActionInput;
import com.stratocloud.provider.tencent.cos.bucket.TencentBucketSpec;
import lombok.Data;

@Data
public class TencentBucketUpdateInput implements ResourceActionInput {
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

    @BooleanField(label = "日志存储")
    private boolean enableLogging;

    @SelectField(label = "目标存储桶", conditions = "this.enableLogging === true")
    private String loggingTargetBucketName;
    @InputField(label = "日志路径前缀", conditions = "this.enableLogging === true", defaultValue = "cos-access-log/")
    private String loggingFilePrefix;


    @JsonIgnore
    public TencentBucketSpec toSpec(){
        TencentBucketSpec bucketSpec = new TencentBucketSpec();
        bucketSpec.setEnableVersioning(enableVersioning);

        bucketSpec.setEnableIntelligentTier(enableIntelligentTier);
        bucketSpec.setDefaultIntelligentTierDays(defaultIntelligentTierDays);

        bucketSpec.setEnableLogging(enableLogging);
        bucketSpec.setLoggingTargetBucketName(loggingTargetBucketName);
        bucketSpec.setLoggingFilePrefix(loggingFilePrefix);

        return bucketSpec;
    }

    public static TencentBucketUpdateInput fromSpec(TencentBucketSpec bucketSpec){
        TencentBucketUpdateInput input = new TencentBucketUpdateInput();
        input.setEnableVersioning(bucketSpec.isEnableVersioning());
        input.setEnableIntelligentTier(bucketSpec.isEnableIntelligentTier());
        input.setDefaultIntelligentTierDays(bucketSpec.getDefaultIntelligentTierDays());
        input.setEnableLogging(bucketSpec.isEnableLogging());
        input.setLoggingTargetBucketName(bucketSpec.getLoggingTargetBucketName());
        input.setLoggingFilePrefix(bucketSpec.getLoggingFilePrefix());
        return input;
    }
}
