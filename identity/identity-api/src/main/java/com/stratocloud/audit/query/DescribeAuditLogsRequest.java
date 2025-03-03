package com.stratocloud.audit.query;

import com.stratocloud.audit.AuditLogLevel;
import com.stratocloud.request.query.PagingRequest;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DescribeAuditLogsRequest extends PagingRequest {
    private String search;
    private List<Long> tenantIds;
    private List<Long> userIds;
    private List<AuditLogLevel> levels;
    private List<Integer> statusCodes;
}
