package com.stratocloud.controllers;

import com.stratocloud.audit.SendAuditLog;
import com.stratocloud.notification.NotificationPolicyApi;
import com.stratocloud.notification.NotificationPolicyService;
import com.stratocloud.notification.cmd.CreateNotificationPolicyCmd;
import com.stratocloud.notification.cmd.DeleteNotificationPoliciesCmd;
import com.stratocloud.notification.cmd.UpdateNotificationPolicyCmd;
import com.stratocloud.notification.query.*;
import com.stratocloud.notification.response.CreateNotificationPolicyResponse;
import com.stratocloud.notification.response.DeleteNotificationPoliciesResponse;
import com.stratocloud.notification.response.UpdateNotificationPolicyResponse;
import com.stratocloud.permission.*;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@PermissionTarget(target = "NotificationPolicy", targetName = "通知策略")
public class NotificationPolicyController implements NotificationPolicyApi {

    private final NotificationPolicyService service;

    public NotificationPolicyController(NotificationPolicyService service) {
        this.service = service;
    }

    @Override
    @SendAuditLog(
            action = "CreateNotificationPolicy",
            actionName = "新建通知策略",
            objectType = "NotificationPolicy",
            objectTypeName = "通知策略"
    )
    @CreatePermissionRequired
    public CreateNotificationPolicyResponse createNotificationPolicy(@RequestBody CreateNotificationPolicyCmd cmd) {
        return service.createNotificationPolicy(cmd);
    }

    @Override
    @SendAuditLog(
            action = "UpdateNotificationPolicy",
            actionName = "更新通知策略",
            objectType = "NotificationPolicy",
            objectTypeName = "通知策略"
    )
    @UpdatePermissionRequired
    public UpdateNotificationPolicyResponse updateNotificationPolicy(@RequestBody UpdateNotificationPolicyCmd cmd) {
        return service.updateNotificationPolicy(cmd);
    }

    @Override
    @SendAuditLog(
            action = "DeleteNotificationPolicies",
            actionName = "删除通知策略",
            objectType = "NotificationPolicy",
            objectTypeName = "通知策略"
    )
    @DeletePermissionRequired
    public DeleteNotificationPoliciesResponse deleteNotificationPolicies(@RequestBody DeleteNotificationPoliciesCmd cmd) {
        return service.deleteNotificationPolicies(cmd);
    }

    @Override
    @ReadPermissionRequired
    public Page<NestedNotificationPolicy> describeNotificationPolicies(@RequestBody DescribeNotificationPoliciesRequest request) {
        return service.describeNotificationPolicies(request);
    }

    @Override
    public DescribeNotificationEventTypesResponse describeNotificationEventTypes(@RequestBody DescribeNotificationEventTypesRequest request) {
        return service.describeNotificationEventTypes(request);
    }
}
