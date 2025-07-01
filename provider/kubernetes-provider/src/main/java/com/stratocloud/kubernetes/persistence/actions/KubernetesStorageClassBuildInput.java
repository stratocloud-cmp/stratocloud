package com.stratocloud.kubernetes.persistence.actions;

import com.stratocloud.form.CodeBlockField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class KubernetesStorageClassBuildInput implements ResourceActionInput {

    @CodeBlockField(label = "对象定义", language = "yaml", defaultValue = NODE_YAML_EXAMPLE)
    private String yamlContent;


    public static final String NODE_YAML_EXAMPLE = """
            apiVersion: storage.k8s.io/v1
            kind: StorageClass
            metadata:
              name: low-latency
              annotations:
                storageclass.kubernetes.io/is-default-class: "false"
            provisioner: csi-driver.example-vendor.example
            reclaimPolicy: Retain # 默认值是 Delete
            allowVolumeExpansion: true
            mountOptions:
              - discard # 这可能会在块存储层启用 UNMAP/TRIM
            volumeBindingMode: WaitForFirstConsumer
            parameters:
              guaranteedReadWriteLatency: "true" # 这是服务提供商特定的
            """;
}
