package com.stratocloud.provider.huawei.obs.actions;

import com.stratocloud.form.BooleanField;
import com.stratocloud.form.InputField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class HuaweiBucketUpdateLoggingInput implements ResourceActionInput {
    @BooleanField(label = "开启日志转存")
    private boolean enabled;

    @SelectField(label = "目标存储桶", conditions = "this.enabled === true")
    private String targetBucket;
    @InputField(label = "日志文件前缀", conditions = "this.enabled === true")
    private String targetPrefix;


    @InputField(label = "IAM委托", conditions = "this.enabled === true")
    private String agency;
}
