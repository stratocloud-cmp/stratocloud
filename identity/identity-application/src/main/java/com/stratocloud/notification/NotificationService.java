package com.stratocloud.notification;

import com.stratocloud.notification.cmd.ResendNotificationCmd;
import com.stratocloud.notification.query.DescribeNotificationsRequest;
import com.stratocloud.notification.query.NestedNotification;
import com.stratocloud.notification.response.ResendNotificationResponse;
import org.springframework.data.domain.Page;

public interface NotificationService {
    Page<NestedNotification> describeNotification(DescribeNotificationsRequest request);

    ResendNotificationResponse resendNotification(ResendNotificationCmd cmd);
}
