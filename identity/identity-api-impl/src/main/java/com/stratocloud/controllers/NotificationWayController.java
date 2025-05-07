package com.stratocloud.controllers;

import com.stratocloud.audit.SendAuditLog;
import com.stratocloud.notification.NotificationWayApi;
import com.stratocloud.notification.NotificationWayService;
import com.stratocloud.notification.cmd.CreateNotificationWayCmd;
import com.stratocloud.notification.cmd.DeleteNotificationWaysCmd;
import com.stratocloud.notification.cmd.UpdateNotificationWayCmd;
import com.stratocloud.notification.query.DescribeNotificationProvidersRequest;
import com.stratocloud.notification.query.DescribeNotificationProvidersResponse;
import com.stratocloud.notification.query.DescribeNotificationWaysRequest;
import com.stratocloud.notification.query.NestedNotificationWay;
import com.stratocloud.notification.response.CreateNotificationWayResponse;
import com.stratocloud.notification.response.DeleteNotificationWaysResponse;
import com.stratocloud.notification.response.UpdateNotificationWayResponse;
import com.stratocloud.permission.*;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PermissionTarget(target = "NotificationWay", targetName = "通知方式")
public class NotificationWayController implements NotificationWayApi {

    private final NotificationWayService service;

    public NotificationWayController(NotificationWayService service) {
        this.service = service;
    }

    @Override
    @SendAuditLog(
            action = "CreateNotificationWay",
            actionName = "新建通知方式",
            objectType = "NotificationWay",
            objectTypeName = "通知方式"
    )
    @CreatePermissionRequired
    public CreateNotificationWayResponse createNotificationWay(@RequestBody CreateNotificationWayCmd cmd) {
        return service.createNotificationWay(cmd);
    }

    @Override
    @SendAuditLog(
            action = "UpdateNotificationWay",
            actionName = "更新通知方式",
            objectType = "NotificationWay",
            objectTypeName = "通知方式"
    )
    @UpdatePermissionRequired
    public UpdateNotificationWayResponse updateNotificationWay(@RequestBody UpdateNotificationWayCmd cmd) {
        return service.updateNotificationWay(cmd);
    }

    @Override
    @SendAuditLog(
            action = "DeleteNotificationWays",
            actionName = "删除通知方式",
            objectType = "NotificationWay",
            objectTypeName = "通知方式"
    )
    @DeletePermissionRequired
    public DeleteNotificationWaysResponse deleteNotificationWays(@RequestBody DeleteNotificationWaysCmd cmd) {
        return service.deleteNotificationWays(cmd);
    }

    @Override
    @ReadPermissionRequired
    public Page<NestedNotificationWay> describeNotificationWays(@RequestBody DescribeNotificationWaysRequest request) {
        return service.describeNotificationWays(request);
    }

    @Override
    public DescribeNotificationProvidersResponse describeNotificationProviders(DescribeNotificationProvidersRequest request) {
        return service.describeNotificationProviders(request);
    }
}
