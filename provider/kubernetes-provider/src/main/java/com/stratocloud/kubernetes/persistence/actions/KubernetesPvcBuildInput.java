package com.stratocloud.kubernetes.persistence.actions;

import com.stratocloud.form.CodeBlockField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class KubernetesPvcBuildInput implements ResourceActionInput {

    @CodeBlockField(label = "对象定义", language = "yaml", defaultValue = NODE_YAML_EXAMPLE)
    private String yamlContent;


    public static final String NODE_YAML_EXAMPLE = """
            apiVersion: v1
            kind: PersistentVolumeClaim
            metadata:
              name: foo-pvc
              namespace: foo
            spec:
              storageClassName: "" # 此处须显式设置空字符串，否则会被设置为默认的 StorageClass
              volumeName: foo-pv
            """;
}
