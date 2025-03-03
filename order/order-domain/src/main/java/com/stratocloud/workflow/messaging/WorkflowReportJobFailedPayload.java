package com.stratocloud.workflow.messaging;

public record WorkflowReportJobFailedPayload(Long nodeInstanceId, Long jobId) {
}
