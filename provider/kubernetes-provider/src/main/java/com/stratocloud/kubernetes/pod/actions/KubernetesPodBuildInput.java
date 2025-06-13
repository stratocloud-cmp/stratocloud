package com.stratocloud.kubernetes.pod.actions;

import com.stratocloud.form.CodeBlockField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class KubernetesPodBuildInput implements ResourceActionInput {

    @CodeBlockField(label = "对象定义", language = "yaml", defaultValue = NODE_YAML_EXAMPLE)
    private String yamlContent;


    public static final String NODE_YAML_EXAMPLE = """
            apiVersion: v1
            kind: Pod
            metadata:
              name: myapp-pod
              labels:
                app: myapp
            spec:
              containers:
              - name: myapp-container
                image: nginx
                ports:
                - containerPort: 80
            """;
}
