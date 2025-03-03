package com.stratocloud.workflow.messaging;

public record WorkflowReportJobFinishedPayload(Long nodeInstanceId, Long jobId) {
}
