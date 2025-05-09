package com.stratocloud.notification.query;

import com.stratocloud.notification.NotificationSendState;
import com.stratocloud.request.query.NestedAuditable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NestedNotificationReceiver extends NestedAuditable {
    private Long receiverUserId;
    private String receiverUserRealName;
    private int successfullySentCount;
    private NotificationSendState state;
    private String errorMessage;
}
