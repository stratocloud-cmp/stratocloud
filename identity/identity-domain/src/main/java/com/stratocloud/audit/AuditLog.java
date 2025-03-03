package com.stratocloud.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.stratocloud.jpa.entities.Tenanted;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Map;

@Slf4j
@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog extends Tenanted {
    @Column(nullable = false)
    private String action;
    @Column(nullable = false)
    private String actionName;
    @Column
    private String objectType;
    @Column
    private String objectTypeName;
    @Column(columnDefinition = "TEXT")
    private String objectIds;
    @Column(columnDefinition = "TEXT")
    private String objectNames;
    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    private AuditLogLevel level;
    @Column
    private String requestId;
    @Column(nullable = false)
    private Long userId;
    @Column(nullable = false)
    private String userLoginName;
    @Column(nullable = false)
    private String userRealName;
    @Column(nullable = false)
    private LocalDateTime requestedAt;
    @Column
    private String sourceIp;
    @Column
    private String path;
    @Convert(converter = AuditLogEncryptConverter.class)
    @Column(columnDefinition = "TEXT")
    private String requestBody;
    @Convert(converter = AuditLogEncryptConverter.class)
    @Column(columnDefinition = "TEXT")
    private String responseBody;
    @Column(nullable = false)
    private Integer statusCode;

    public AuditLog(AuditLogPayload payload){
        setTenantId(payload.requestRecord().tenantId());

        this.action = payload.action();
        this.actionName = payload.actionName();

        this.objectType = payload.objectType();
        this.objectTypeName = payload.objectTypeName();

        if(Utils.isNotEmpty(payload.objectIds()))
            this.objectIds = String.join(",", payload.objectIds());
        if(Utils.isNotEmpty(payload.objectNames()))
            this.objectNames = String.join(",", payload.objectNames());

        this.level = payload.level();

        this.requestId = payload.requestRecord().requestId();
        this.userId = payload.requestRecord().requestedBy().userId();
        this.userLoginName = payload.requestRecord().requestedBy().loginName();
        this.userRealName = payload.requestRecord().requestedBy().realName();

        this.requestedAt = payload.requestRecord().requestedAt();
        this.sourceIp = payload.requestRecord().sourceIp();
        this.path = payload.requestRecord().path();
        this.requestBody = eraseSensitiveProperties(payload.requestRecord().requestBody());
        this.responseBody = eraseSensitiveProperties(payload.requestRecord().responseBody());
        this.statusCode = payload.requestRecord().statusCode();
    }

    private String eraseSensitiveProperties(String json){
        if(Utils.isBlank(json))
            return json;

        try {
            JsonNode jsonNode = JSON.getObjectMapper().readTree(json);
            eraseSensitiveProperties(jsonNode);
            return jsonNode.toPrettyString();
        } catch (Exception e) {
            log.warn("Failed to erase sensitive properties, returning empty string");
            return "";
        }
    }

    private void eraseSensitiveProperties(JsonNode jsonNode){
        if(jsonNode instanceof ArrayNode arrayNode){
            arrayNode.elements().forEachRemaining(this::eraseSensitiveProperties);
        }else if(jsonNode instanceof ObjectNode objectNode){
            Iterator<Map.Entry<String, JsonNode>> iterator = objectNode.fields();
            while (iterator.hasNext()){
                Map.Entry<String, JsonNode> entry = iterator.next();
                if(isSensitiveProperty(entry.getKey()))
                    iterator.remove();
                else
                    eraseSensitiveProperties(entry.getValue());
            }
        }
    }

    private boolean isSensitiveProperty(String propertyName){
        return Utils.isNotBlank(propertyName) &&
                propertyName.toLowerCase().contains("pass") &&
                propertyName.toLowerCase().contains("key") &&
                propertyName.toLowerCase().contains("secret");
    }
}
