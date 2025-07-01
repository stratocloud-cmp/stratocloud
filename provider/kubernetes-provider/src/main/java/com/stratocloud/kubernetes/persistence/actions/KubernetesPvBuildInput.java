package com.stratocloud.kubernetes.persistence.actions;

import com.stratocloud.form.CodeBlockField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class KubernetesPvBuildInput implements ResourceActionInput {

    @CodeBlockField(label = "对象定义", language = "yaml", defaultValue = NODE_YAML_EXAMPLE)
    private String yamlContent;


    public static final String NODE_YAML_EXAMPLE = """
            apiVersion: v1
            kind: PersistentVolume
            metadata:
              name: pv0003
            spec:
              capacity:
                storage: 5Gi
              volumeMode: Filesystem
              accessModes:
                - ReadWriteOnce
              persistentVolumeReclaimPolicy: Recycle
              storageClassName: slow
              mountOptions:
                - hard
                - nfsvers=4.1
              nfs:
                path: /tmp
                server: 172.17.0.2
            """;
}
