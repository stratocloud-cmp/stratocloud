package com.stratocloud.kubernetes.job.actions;

import com.stratocloud.form.CodeBlockField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class KubernetesJobBuildInput implements ResourceActionInput {

    @CodeBlockField(label = "对象定义", language = "yaml", defaultValue = NODE_YAML_EXAMPLE)
    private String yamlContent;


    public static final String NODE_YAML_EXAMPLE = """
            apiVersion: batch/v1
            kind: Job
            metadata:
              name: pi
            spec:
              template:
                spec:
                  containers:
                  - name: pi
                    image: perl:5.34.0
                    command: ["perl",  "-Mbignum=bpi", "-wle", "print bpi(2000)"]
                  restartPolicy: Never
              backoffLimit: 4
            """;
}
