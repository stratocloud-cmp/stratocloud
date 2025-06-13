package com.stratocloud.kubernetes.network.actions;

import com.stratocloud.form.CodeBlockField;
import com.stratocloud.provider.resource.ResourceActionInput;
import lombok.Data;

@Data
public class KubernetesNetworkPolicyBuildInput implements ResourceActionInput {

    @CodeBlockField(label = "对象定义", language = "yaml", defaultValue = NETWORK_POLICY_YAML_EXAMPLE)
    private String yamlContent;


    public static final String NETWORK_POLICY_YAML_EXAMPLE = """
            apiVersion: networking.k8s.io/v1
            kind: NetworkPolicy
            metadata:
              name: test-network-policy
              namespace: default
            spec:
              podSelector:
                matchLabels:
                  role: db
              policyTypes:
              - Ingress
              - Egress
              ingress:
              - from:
                - ipBlock:
                    cidr: 172.17.0.0/16
                    except:
                    - 172.17.1.0/24
                - namespaceSelector:
                    matchLabels:
                      project: myproject
                - podSelector:
                    matchLabels:
                      role: frontend
                ports:
                - protocol: TCP
                  port: 6379
              egress:
              - to:
                - ipBlock:
                    cidr: 10.0.0.0/24
                ports:
                - protocol: TCP
                  port: 5978
            """;
}
