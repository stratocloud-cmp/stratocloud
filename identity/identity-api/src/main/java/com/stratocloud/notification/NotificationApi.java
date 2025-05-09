package com.stratocloud.notification;

import com.stratocloud.constant.StratoServices;
import com.stratocloud.notification.cmd.ResendNotificationCmd;
import com.stratocloud.notification.query.DescribeNotificationsRequest;
import com.stratocloud.notification.query.NestedNotification;
import com.stratocloud.notification.response.ResendNotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface NotificationApi {
    @PostMapping(StratoServices.IDENTITY_SERVICE+"/describe-notifications")
    Page<NestedNotification> describeNotifications(@RequestBody DescribeNotificationsRequest request);

    @PostMapping(StratoServices.IDENTITY_SERVICE+"/resend-notification")
    ResendNotificationResponse resendNotification(@RequestBody ResendNotificationCmd cmd);
}
