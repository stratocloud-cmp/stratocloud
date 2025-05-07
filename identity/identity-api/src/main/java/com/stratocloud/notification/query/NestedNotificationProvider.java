package com.stratocloud.notification.query;

import com.stratocloud.form.info.DynamicFormMetaData;
import lombok.Data;

@Data
public class NestedNotificationProvider {
    private String providerId;
    private String providerName;
    private DynamicFormMetaData formMetaData;
}
