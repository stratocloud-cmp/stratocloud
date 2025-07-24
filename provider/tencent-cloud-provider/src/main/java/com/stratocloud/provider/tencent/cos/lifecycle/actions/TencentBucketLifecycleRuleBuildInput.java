package com.stratocloud.provider.tencent.cos.lifecycle.actions;

import com.stratocloud.form.BooleanField;
import com.stratocloud.form.InputField;
import com.stratocloud.form.SelectField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class TencentBucketLifecycleRuleBuildInput implements ResourceActionInput {
    @BooleanField(label = "开启")
    private boolean enabled;
    @InputField(label = "规则名称")
    private String ruleId;

    @SelectField(
            label = "应用范围",
            options = {
                    "filtered",
                    "unfiltered"
            },
            optionNames = {
                    "指定范围",
                    "整个存储桶"
            },
            defaultValues = "filtered"
    )
    private String filterOption;

    @BooleanField(label = "指定对象前缀", defaultValue = true, conditions = "this.filterOption === 'filtered'")
    private boolean enablePrefix;

    @InputField(label = "对象前缀", conditions = "this.filterOption === 'filtered' || this.enablePrefix === true")
    private String prefix;


}
