package com.stratocloud.audit;

import com.stratocloud.audit.query.DescribeAuditLogsRequest;
import com.stratocloud.audit.query.NestedAuditLog;
import org.springframework.data.domain.Page;

public interface AuditLogService {
    Page<NestedAuditLog> describeAuditLogs(DescribeAuditLogsRequest request);
}
