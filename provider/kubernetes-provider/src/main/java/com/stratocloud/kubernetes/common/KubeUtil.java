package com.stratocloud.kubernetes.common;

import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.utils.Assert;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.util.Yaml;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KubeUtil {
    public static String getObjectName(V1ObjectMeta objectMeta){
        Assert.isNotNull(objectMeta);
        return objectMeta.getName();
    }

    public static NamespacedRef getNamespacedRef(V1ObjectMeta objectMeta) {
        Assert.isNotNull(objectMeta);
        return new NamespacedRef(objectMeta.getNamespace(), objectMeta.getName());
    }

    public static <T extends KubernetesObject> T fromYaml(String yamlContent, Class<T> clazz){
        try {
            return Yaml.loadAs(yamlContent, clazz);
        }catch (Exception e){
            log.error("Failed to load yaml:\n {}", yamlContent, e);
            throw new BadCommandException("YAML内容有误");
        }
    }

    public static String toYaml(KubernetesObject object){
        return Yaml.dump(object);
    }
}
