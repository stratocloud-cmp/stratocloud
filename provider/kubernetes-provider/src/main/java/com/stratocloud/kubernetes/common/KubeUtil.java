package com.stratocloud.kubernetes.common;

import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.form.info.CodeBlockFieldDetail;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.form.info.FieldInfo;
import com.stratocloud.utils.Assert;
import com.stratocloud.utils.Utils;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1OwnerReference;
import io.kubernetes.client.util.Yaml;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

    public static Optional<V1OwnerReference> getOwnerReference(V1ObjectMeta metadata, String targetKind){
        if(metadata == null)
            return Optional.empty();

        List<V1OwnerReference> ownerReferences = metadata.getOwnerReferences();

        if(Utils.isEmpty(ownerReferences))
            return Optional.empty();

        return ownerReferences.stream().filter(
                r -> Objects.equals(r.getKind(), targetKind)
        ).findAny();
    }

    public static DynamicFormMetaData replaceYamlContent(DynamicFormMetaData formMetaData,
                                                         String yamlContent) {
        List<FieldInfo> fieldInfoList = new ArrayList<>();

        if(Utils.isNotEmpty(formMetaData.fieldInfoList())){
            for (FieldInfo fieldInfo : formMetaData.fieldInfoList()) {
                if("yamlContent".equals(fieldInfo.key()) && fieldInfo.detail() instanceof CodeBlockFieldDetail c){

                    FieldInfo newFieldInfo = new FieldInfo(
                            fieldInfo.type(),
                            fieldInfo.key(),
                            fieldInfo.label(),
                            fieldInfo.description(),
                            new CodeBlockFieldDetail(
                                    yamlContent,
                                    c.required(),
                                    c.conditions(),
                                    c.language()
                            )
                    );
                    fieldInfoList.add(newFieldInfo);
                } else {
                    fieldInfoList.add(fieldInfo);
                }
            }
        }

        return new DynamicFormMetaData(
                formMetaData.formClass(),
                fieldInfoList
        );
    }
}
