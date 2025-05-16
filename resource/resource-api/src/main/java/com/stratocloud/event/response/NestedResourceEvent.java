package com.stratocloud.event.response;

import com.stratocloud.event.StratoEventLevel;
import com.stratocloud.event.StratoEventSource;
import com.stratocloud.request.query.NestedControllable;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
public class NestedResourceEvent extends NestedControllable {
    private String internalEventId;
    private String externalEventId;
    private String eventType;
    private String eventTypeName;
    private StratoEventLevel level;
    private StratoEventSource source;
    private String summary;
    private LocalDateTime eventHappenedAt;
    private Map<String,Object> eventProperties;
    private String providerId;
    private String providerName;
    private Long accountId;
    private String accountName;
    private String resourceCategory;
    private String resourceCategoryName;
    private String resourceTypeId;
    private String resourceTypeName;
    private Long resourceId;
    private String resourceName;
}
