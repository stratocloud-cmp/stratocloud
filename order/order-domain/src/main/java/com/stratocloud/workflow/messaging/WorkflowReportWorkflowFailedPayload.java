package com.stratocloud.workflow.messaging;

public record WorkflowReportWorkflowFailedPayload(Long workflowInstanceId, String errorMessage) {
}
