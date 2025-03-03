package com.stratocloud.audit.query;

import com.stratocloud.audit.AuditLogLevel;
import com.stratocloud.request.query.NestedTenanted;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class NestedAuditLog extends NestedTenanted {
    private String action;
    private String actionName;
    private String objectType;
    private String objectTypeName;
    private String objectIds;
    private String objectNames;
    private AuditLogLevel level;
    private String requestId;
    private Long userId;
    private String userLoginName;
    private String userRealName;
    private LocalDateTime requestedAt;
    private String sourceIp;
    private String path;
    private String requestBody;
    private String responseBody;
    private Integer statusCode;
}
