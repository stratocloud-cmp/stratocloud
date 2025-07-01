package com.stratocloud.kubernetes.stateful.actions;

import com.stratocloud.form.CodeBlockField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class KubernetesStatefulSetUpdateInput implements ResourceActionInput {
    @CodeBlockField(label = "对象定义", language = "yaml")
    private String yamlContent;
}
