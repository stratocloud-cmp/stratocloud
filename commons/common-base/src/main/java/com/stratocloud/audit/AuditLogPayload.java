package com.stratocloud.audit;

import com.stratocloud.request.RequestRecord;

import java.util.List;

public record AuditLogPayload(String action,
                              String actionName,
                              String objectType,
                              String objectTypeName,
                              List<String> objectIds,
                              List<String> objectNames,
                              AuditLogLevel level,
                              RequestRecord requestRecord) {
}
