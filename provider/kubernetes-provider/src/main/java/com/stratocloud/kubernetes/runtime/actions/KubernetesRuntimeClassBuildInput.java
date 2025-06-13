package com.stratocloud.kubernetes.runtime.actions;

import com.stratocloud.form.CodeBlockField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class KubernetesRuntimeClassBuildInput implements ResourceActionInput {

    @CodeBlockField(label = "对象定义", language = "yaml", defaultValue = RUNTIME_CLASS_YAML_EXAMPLE)
    private String yamlContent;


    public static final String RUNTIME_CLASS_YAML_EXAMPLE = """
            apiVersion: node.k8s.io/v1
            kind: RuntimeClass
            metadata:
              name: example-name
            handler: example-handler
            scheduling:
              nodeSelector:
                example-key: example-value
              tolerations:
              - key: "example"
                operator: "Exists"
                effect: "NoSchedule"
            overhead:
              podFixed:
                cpu: "500m"
                memory: "128Mi"
            """;
}
