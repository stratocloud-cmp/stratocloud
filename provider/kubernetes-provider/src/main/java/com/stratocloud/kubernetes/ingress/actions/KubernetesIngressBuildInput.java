package com.stratocloud.kubernetes.ingress.actions;

import com.stratocloud.form.CodeBlockField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class KubernetesIngressBuildInput implements ResourceActionInput {

    @CodeBlockField(label = "对象定义", language = "yaml", defaultValue = NODE_YAML_EXAMPLE)
    private String yamlContent;


    public static final String NODE_YAML_EXAMPLE = """
            apiVersion: networking.k8s.io/v1
            kind: Ingress
            metadata:
              name: minimal-ingress
              annotations:
                nginx.ingress.kubernetes.io/rewrite-target: /
            spec:
              ingressClassName: nginx-example
              rules:
              - http:
                  paths:
                  - path: /testpath
                    pathType: Prefix
                    backend:
                      service:
                        name: test
                        port:
                          number: 80
            """;
}
