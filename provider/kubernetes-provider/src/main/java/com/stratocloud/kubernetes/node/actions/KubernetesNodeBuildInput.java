package com.stratocloud.kubernetes.node.actions;

import com.stratocloud.form.CodeBlockField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class KubernetesNodeBuildInput implements ResourceActionInput {

    @CodeBlockField(label = "对象定义", language = "yaml", defaultValue = NODE_YAML_EXAMPLE)
    private String yamlContent;


    public static final String NODE_YAML_EXAMPLE = """
            apiVersion: node.k8s.io/v1
            kind: Node
            metadata:
              name: example-name
              labels:
                name: example-name
            """;
}
