package com.stratocloud.resource.event;

import com.stratocloud.event.StratoEvent;
import com.stratocloud.event.StratoEventLevel;
import com.stratocloud.event.StratoEventSource;
import com.stratocloud.jpa.entities.Controllable;
import com.stratocloud.utils.JSON;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Getter
@Setter
public class ResourceEvent extends Controllable {
    @Column(nullable = false)
    private String internalEventId;
    @Column
    private String externalEventId;
    @Column(nullable = false)
    private String eventType;
    @Column(nullable = false)
    private String eventTypeName;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StratoEventLevel level;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StratoEventSource source;
    @Column(columnDefinition = "TEXT")
    private String summary;
    @Column(nullable = false, updatable = false)
    private LocalDateTime eventHappenedAt;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column
    private Map<String,Object> eventProperties;
    @Column(nullable = false)
    private String providerId;
    @Column(nullable = false)
    private String providerName;
    @Column(nullable = false)
    private Long accountId;
    @Column(nullable = false)
    private String accountName;
    @Column(nullable = false)
    private String resourceCategory;
    @Column(nullable = false)
    private String resourceCategoryName;
    @Column(nullable = false)
    private String resourceTypeId;
    @Column(nullable = false)
    private String resourceTypeName;
    @Column(nullable = false)
    private Long resourceId;
    @Column(nullable = false)
    private String resourceName;


    public static ResourceEvent from(StratoEvent<? extends ResourceEventProperties> event) {
        ResourceEvent resourceEvent = new ResourceEvent();
        resourceEvent.setInternalEventId(event.id());
        resourceEvent.setEventType(event.type().id());
        resourceEvent.setEventTypeName(event.type().name());
        resourceEvent.setLevel(event.level());
        resourceEvent.setSource(event.source());
        resourceEvent.setSummary(event.summary());
        resourceEvent.setEventHappenedAt(event.eventHappenedAt());
        resourceEvent.setEventProperties(JSON.toMap(event.properties()));
        resourceEvent.setProviderId(event.properties().getProviderId());
        resourceEvent.setProviderName(event.properties().getProviderName());
        resourceEvent.setAccountId(event.properties().getAccountId());
        resourceEvent.setAccountName(event.properties().getAccountName());
        resourceEvent.setResourceCategory(event.properties().getResourceCategory().id());
        resourceEvent.setResourceCategoryName(event.properties().getResourceCategory().name());
        resourceEvent.setResourceTypeId(event.properties().getResourceTypeId());
        resourceEvent.setResourceTypeName(event.properties().getResourceTypeName());
        resourceEvent.setResourceId(event.properties().getResourceId());
        resourceEvent.setResourceName(event.properties().getResourceName());
        resourceEvent.setOwnerId(event.properties().getResourceOwnerId());
        resourceEvent.setTenantId(event.properties().getResourceTenantId());
        return resourceEvent;
    }
}
