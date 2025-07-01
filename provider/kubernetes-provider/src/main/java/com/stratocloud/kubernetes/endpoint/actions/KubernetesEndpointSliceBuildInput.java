package com.stratocloud.kubernetes.endpoint.actions;

import com.stratocloud.form.CodeBlockField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class KubernetesEndpointSliceBuildInput implements ResourceActionInput {

    @CodeBlockField(label = "对象定义", language = "yaml", defaultValue = NODE_YAML_EXAMPLE)
    private String yamlContent;


    public static final String NODE_YAML_EXAMPLE = """
            apiVersion: discovery.k8s.io/v1
            kind: EndpointSlice
            metadata:
              name: example-abc
              labels:
                kubernetes.io/service-name: example
            addressType: IPv4
            ports:
              - name: http
                protocol: TCP
                port: 80
            endpoints:
              - addresses:
                  - "10.1.2.3"
                conditions:
                  ready: true
                hostname: pod-1
                nodeName: node-1
                zone: us-west2-a
            """;
}
