package com.stratocloud.workflow.messaging;

public record WorkflowReportJobStartedPayload(Long nodeInstanceId, Long jobId) {
}
