package com.stratocloud.kubernetes.job.actions;

import com.stratocloud.form.CodeBlockField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class KubernetesCronJobBuildInput implements ResourceActionInput {

    @CodeBlockField(label = "对象定义", language = "yaml", defaultValue = NODE_YAML_EXAMPLE)
    private String yamlContent;


    public static final String NODE_YAML_EXAMPLE = """
            apiVersion: batch/v1
            kind: CronJob
            metadata:
              name: hello
            spec:
              schedule: "* * * * *"
              jobTemplate:
                spec:
                  template:
                    spec:
                      containers:
                      - name: hello
                        image: busybox:1.28
                        imagePullPolicy: IfNotPresent
                        command:
                        - /bin/sh
                        - -c
                        - date; echo Hello from the Kubernetes cluster
                      restartPolicy: OnFailure
            """;
}
