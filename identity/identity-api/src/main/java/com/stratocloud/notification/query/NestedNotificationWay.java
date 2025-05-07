package com.stratocloud.notification.query;

import com.stratocloud.notification.NotificationProviderStatus;
import com.stratocloud.request.query.NestedTenanted;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class NestedNotificationWay extends NestedTenanted {
    private String providerId;
    private String providerName;

    private String name;
    private String description;

    private Map<String, Object> properties;

    private NotificationProviderStatus providerStatus;

    private String errorMessage;
}
