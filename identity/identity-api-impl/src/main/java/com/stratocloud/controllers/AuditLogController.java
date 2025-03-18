package com.stratocloud.controllers;

import com.stratocloud.audit.AuditLogApi;
import com.stratocloud.audit.AuditLogService;
import com.stratocloud.audit.query.DescribeAuditLogsRequest;
import com.stratocloud.audit.query.NestedAuditLog;
import com.stratocloud.permission.PermissionTarget;
import com.stratocloud.permission.ReadPermissionRequired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@PermissionTarget(target = "AuditLog", targetName = "审计日志")
@RestController
public class AuditLogController implements AuditLogApi {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Override
    @ReadPermissionRequired
    public Page<NestedAuditLog> describeAuditLogs(@RequestBody DescribeAuditLogsRequest request) {
        return auditLogService.describeAuditLogs(request);
    }
}
