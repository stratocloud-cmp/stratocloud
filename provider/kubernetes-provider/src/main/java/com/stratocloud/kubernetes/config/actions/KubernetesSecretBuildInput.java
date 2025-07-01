package com.stratocloud.kubernetes.config.actions;

import com.stratocloud.form.CodeBlockField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class KubernetesSecretBuildInput implements ResourceActionInput {

    @CodeBlockField(label = "对象定义", language = "yaml", defaultValue = NODE_YAML_EXAMPLE)
    private String yamlContent;


    public static final String NODE_YAML_EXAMPLE = """
            apiVersion: v1
            kind: Secret
            metadata:
              name: dotfile-secret
            data:
              .secret-file: dmFsdWUtMg0KDQo=
            """;
}
