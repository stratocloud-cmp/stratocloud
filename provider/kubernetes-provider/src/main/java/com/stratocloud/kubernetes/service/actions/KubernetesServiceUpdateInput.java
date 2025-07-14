package com.stratocloud.kubernetes.service.actions;

import com.stratocloud.form.CodeBlockField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class KubernetesServiceUpdateInput implements ResourceActionInput {
    @CodeBlockField(label = "对象定义", language = "yaml")
    private String yamlContent;
}
