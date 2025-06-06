package com.stratocloud.kubernetes.common;

import com.stratocloud.form.CodeBlockField;
import com.stratocloud.provider.ExternalAccountProperties;
import lombok.Data;

@Data
public class KubernetesAccountProperties implements ExternalAccountProperties {
    @CodeBlockField(label = "KubeConfig YAML", language = "yaml")
    private String kubeConfigYaml;
}
