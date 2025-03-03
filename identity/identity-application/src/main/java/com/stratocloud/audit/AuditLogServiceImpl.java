package com.stratocloud.audit;

import com.stratocloud.audit.query.DescribeAuditLogsRequest;
import com.stratocloud.audit.query.NestedAuditLog;
import com.stratocloud.jpa.entities.EntityUtil;
import com.stratocloud.repository.AuditLogRepository;
import com.stratocloud.validate.ValidateRequest;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogServiceImpl implements AuditLogService{

    private final AuditLogRepository repository;

    public AuditLogServiceImpl(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Override
    @ValidateRequest
    @Transactional(readOnly = true)
    public Page<NestedAuditLog> describeAuditLogs(DescribeAuditLogsRequest request) {
        Page<AuditLog> page = repository.page(
                request.getSearch(),
                request.getLevels(),
                request.getTenantIds(),
                request.getUserIds(),
                request.getStatusCodes(),
                request.getPageable()
        );

        return page.map(this::toNestedAuditLog);
    }

    private NestedAuditLog toNestedAuditLog(AuditLog auditLog) {
        NestedAuditLog nestedAuditLog = new NestedAuditLog();

        EntityUtil.copyBasicFields(auditLog, nestedAuditLog);

        nestedAuditLog.setAction(auditLog.getAction());
        nestedAuditLog.setActionName(auditLog.getActionName());
        nestedAuditLog.setObjectType(auditLog.getObjectType());
        nestedAuditLog.setObjectTypeName(auditLog.getObjectTypeName());
        nestedAuditLog.setObjectIds(auditLog.getObjectIds());
        nestedAuditLog.setObjectNames(auditLog.getObjectNames());
        nestedAuditLog.setLevel(auditLog.getLevel());
        nestedAuditLog.setRequestId(auditLog.getRequestId());
        nestedAuditLog.setUserId(auditLog.getUserId());
        nestedAuditLog.setUserLoginName(auditLog.getUserLoginName());
        nestedAuditLog.setUserRealName(auditLog.getUserRealName());
        nestedAuditLog.setRequestedAt(auditLog.getRequestedAt());
        nestedAuditLog.setSourceIp(auditLog.getSourceIp());
        nestedAuditLog.setPath(auditLog.getPath());
        nestedAuditLog.setRequestBody(auditLog.getRequestBody());
        nestedAuditLog.setResponseBody(auditLog.getResponseBody());
        nestedAuditLog.setStatusCode(auditLog.getStatusCode());

        return nestedAuditLog;
    }
}
