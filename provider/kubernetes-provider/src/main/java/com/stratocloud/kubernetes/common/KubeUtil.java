package com.stratocloud.kubernetes.common;

import com.stratocloud.utils.Assert;
import io.kubernetes.client.openapi.models.V1ObjectMeta;

public class KubeUtil {
    public static String getObjectName(V1ObjectMeta objectMeta){
        Assert.isNotNull(objectMeta);
        return objectMeta.getName();
    }

    public static NamespacedRef getNamespacedRef(V1ObjectMeta objectMeta) {
        Assert.isNotNull(objectMeta);
        return new NamespacedRef(objectMeta.getNamespace(), objectMeta.getName());
    }
}
