package com.stratocloud.controllers;

import com.stratocloud.audit.SendAuditLog;
import com.stratocloud.notification.NotificationApi;
import com.stratocloud.notification.NotificationService;
import com.stratocloud.notification.cmd.ResendNotificationCmd;
import com.stratocloud.notification.query.DescribeNotificationsRequest;
import com.stratocloud.notification.query.NestedNotification;
import com.stratocloud.notification.response.ResendNotificationResponse;
import com.stratocloud.permission.PermissionRequired;
import com.stratocloud.permission.PermissionTarget;
import com.stratocloud.permission.ReadPermissionRequired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

@Component
@PermissionTarget(target = "Notification", targetName = "通知记录")
public class NotificationController implements NotificationApi {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @Override
    @ReadPermissionRequired
    public Page<NestedNotification> describeNotifications(@RequestBody DescribeNotificationsRequest request) {
        return service.describeNotification(request);
    }

    @Override
    @PermissionRequired(action = "RESEND", actionName = "重新发送")
    @SendAuditLog(
            action = "ResendNotification",
            actionName = "重新发送通知",
            objectType = "Notification",
            objectTypeName = "通知记录"
    )
    public ResendNotificationResponse resendNotification(@RequestBody ResendNotificationCmd cmd) {
        return service.resendNotification(cmd);
    }
}
