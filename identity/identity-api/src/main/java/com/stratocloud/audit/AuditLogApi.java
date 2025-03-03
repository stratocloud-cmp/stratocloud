package com.stratocloud.audit;

import com.stratocloud.audit.query.DescribeAuditLogsRequest;
import com.stratocloud.audit.query.NestedAuditLog;
import com.stratocloud.constant.StratoServices;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface AuditLogApi {
    @PostMapping(path = StratoServices.IDENTITY_SERVICE+"/describe-audit-logs")
    Page<NestedAuditLog> describeAuditLogs(@RequestBody DescribeAuditLogsRequest request);
}
