package com.stratocloud.kubernetes.ingress.actions;

import com.stratocloud.form.CodeBlockField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class KubernetesIngressClassBuildInput implements ResourceActionInput {

    @CodeBlockField(label = "对象定义", language = "yaml", defaultValue = NODE_YAML_EXAMPLE)
    private String yamlContent;


    public static final String NODE_YAML_EXAMPLE = """
            apiVersion: networking.k8s.io/v1
            kind: IngressClass
            metadata:
              name: external-lb
            spec:
              controller: example.com/ingress-controller
              parameters:
                apiGroup: k8s.example.com
                kind: IngressParameters
                name: external-lb
            """;
}
