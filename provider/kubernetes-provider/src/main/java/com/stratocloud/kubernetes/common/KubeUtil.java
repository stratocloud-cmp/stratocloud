package com.stratocloud.kubernetes.common;

import com.stratocloud.account.ExternalAccount;
import com.stratocloud.event.ExternalResourceEvent;
import com.stratocloud.event.StratoEventLevel;
import com.stratocloud.event.StratoEventSource;
import com.stratocloud.event.StratoEventType;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.form.info.CodeBlockFieldDetail;
import com.stratocloud.form.info.DynamicFormMetaData;
import com.stratocloud.form.info.FieldInfo;
import com.stratocloud.kubernetes.KubernetesProvider;
import com.stratocloud.utils.Assert;
import com.stratocloud.utils.TimeUtil;
import com.stratocloud.utils.Utils;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.CoreV1Event;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1ObjectReference;
import io.kubernetes.client.openapi.models.V1OwnerReference;
import io.kubernetes.client.util.Yaml;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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

    public static LocalDateTime toLocalDateTime(String kubernetesTime) {
        if(Utils.isBlank(kubernetesTime))
            return LocalDateTime.now();

        return LocalDateTime.parse(kubernetesTime, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")).atZone(
                TimeUtil.UTC_ZONE_ID
        ).withZoneSameInstant(
                ZoneId.systemDefault()
        ).toLocalDateTime();
    }


    public static List<ExternalResourceEvent> describeResourceEvents(KubernetesProvider provider,
                                                                     ExternalAccount account,
                                                                     String objectKind,
                                                                     String resourceTypeId,
                                                                     String externalId,
                                                                     LocalDateTime happenedAfter,
                                                                     boolean isNamespaced) {
        V1ObjectReference ref = new V1ObjectReference();
        ref.setKind(objectKind);
        if(isNamespaced){
            NamespacedRef namespacedRef = NamespacedRef.fromString(externalId);
            ref.setNamespace(namespacedRef.namespace());
            ref.setName(namespacedRef.name());
        }else {
            ref.setName(externalId);
        }


        return provider.buildClient(account).describeEventsByObjectRef(ref, happenedAfter).stream().filter(
                e -> e.getLastTimestamp() != null
        ).map(
                e -> new ExternalResourceEvent(
                        e.getMetadata().getUid(),
                        convertEventType(e),
                        convertEventLevel(e.getType()),
                        StratoEventSource.EXTERNAL_ACTION,
                        resourceTypeId,
                        account.getId(),
                        externalId,
                        getEventMessage(e),
                        e.getLastTimestamp().toLocalDateTime()
                )
        ).toList();
    }

    private static String getEventMessage(CoreV1Event event) {
        String result = "";

        if(Utils.isNotBlank(event.getMessage()))
            result = result+event.getMessage()+"\n";

        result = result+"Count: %s".formatted(
                event.getCount() != null ? event.getCount() : 0
        );

        return result;
    }

    private static StratoEventType convertEventType(CoreV1Event event) {
        String eventType;

        if(Utils.isBlank(event.getReason()) && Utils.isBlank(event.getAction())) {
            eventType = "UNKNOWN";
        } else if(Utils.isBlank(event.getReason()) && Utils.isNotBlank(event.getAction())){
            eventType = event.getAction();
        } else if(Utils.isNotBlank(event.getReason()) && Utils.isBlank(event.getAction())){
            eventType = event.getReason();
        }else {
            eventType = "%s.%s".formatted(event.getAction(), event.getReason());
        }
        return new StratoEventType(eventType, eventType);
    }

    private static StratoEventLevel convertEventLevel(String type) {
        if(Utils.isBlank(type))
            return StratoEventLevel.REMIND;
        return switch (type){
            case "Warning" -> StratoEventLevel.WARNING;
            case "Normal" -> StratoEventLevel.INFO;
            default -> StratoEventLevel.REMIND;
        };
    }
}
