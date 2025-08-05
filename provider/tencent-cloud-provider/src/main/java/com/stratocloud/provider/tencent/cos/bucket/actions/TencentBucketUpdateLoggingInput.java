package com.stratocloud.provider.tencent.cos.bucket.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.qcloud.cos.model.BucketLoggingConfiguration;
import com.stratocloud.form.BooleanField;
import com.stratocloud.form.InputField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class TencentBucketUpdateLoggingInput implements ResourceActionInput {
    @BooleanField(label = "日志存储")
    private boolean enableLogging;

    @SelectField(label = "目标存储桶", conditions = "this.enableLogging === true")
    private String loggingTargetBucketName;
    @InputField(label = "日志路径前缀", conditions = "this.enableLogging === true", defaultValue = "cos-access-log/")
    private String loggingFilePrefix;

    @JsonIgnore
    public BucketLoggingConfiguration toConfig() {
        return new BucketLoggingConfiguration(loggingTargetBucketName, loggingFilePrefix);
    }
}
