package com.stratocloud.kubernetes.network.actions;

import com.stratocloud.form.CodeBlockField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class KubernetesNetworkPolicyUpdateInput implements ResourceActionInput {
    @CodeBlockField(label = "对象定义", language = "yaml")
    private String yamlContent;
}
