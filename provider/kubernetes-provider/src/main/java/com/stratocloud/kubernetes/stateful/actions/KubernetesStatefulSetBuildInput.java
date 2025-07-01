package com.stratocloud.kubernetes.stateful.actions;

import com.stratocloud.form.CodeBlockField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class KubernetesStatefulSetBuildInput implements ResourceActionInput {

    @CodeBlockField(label = "对象定义", language = "yaml", defaultValue = NODE_YAML_EXAMPLE)
    private String yamlContent;


    public static final String NODE_YAML_EXAMPLE = """
            apiVersion: apps/v1
            kind: StatefulSet
            metadata:
              name: web
            spec:
              selector:
                matchLabels:
                  app: nginx # 必须匹配 .spec.template.metadata.labels
              serviceName: "nginx"
              replicas: 3 # 默认值是 1
              minReadySeconds: 10 # 默认值是 0
              template:
                metadata:
                  labels:
                    app: nginx # 必须匹配 .spec.selector.matchLabels
                spec:
                  terminationGracePeriodSeconds: 10
                  containers:
                  - name: nginx
                    image: registry.k8s.io/nginx-slim:0.24
                    ports:
                    - containerPort: 80
                      name: web
                    volumeMounts:
                    - name: www
                      mountPath: /usr/share/nginx/html
              volumeClaimTemplates:
              - metadata:
                  name: www
                spec:
                  accessModes: [ "ReadWriteOnce" ]
                  storageClassName: "my-storage-class"
                  resources:
                    requests:
                      storage: 1Gi
            """;
}
