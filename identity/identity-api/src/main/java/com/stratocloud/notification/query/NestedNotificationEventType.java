package com.stratocloud.notification.query;

import com.stratocloud.request.query.NestedAuditable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NestedNotificationEventType extends NestedAuditable {
    private String eventType;
    private String eventTypeName;
    private String eventPropertiesExample;
}
