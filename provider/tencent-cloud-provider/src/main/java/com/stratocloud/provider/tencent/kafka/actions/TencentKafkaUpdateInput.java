package com.stratocloud.provider.tencent.kafka.actions;

import com.stratocloud.form.InputField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class TencentKafkaUpdateInput implements ResourceActionInput {
    @InputField(label = "实例名称")
    private String instanceName;
}
