package com.stratocloud.kubernetes.service.actions;

import com.stratocloud.form.CodeBlockField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class KubernetesServiceBuildInput implements ResourceActionInput {

    @CodeBlockField(label = "对象定义", language = "yaml", defaultValue = NODE_YAML_EXAMPLE)
    private String yamlContent;


    public static final String NODE_YAML_EXAMPLE = """
            apiVersion: v1
            kind: Service
            metadata:
              name: my-service
            spec:
              selector:
                app.kubernetes.io/name: nginx
              ports:
                - protocol: TCP
                  port: 80
                  targetPort: 80
            """;
}
