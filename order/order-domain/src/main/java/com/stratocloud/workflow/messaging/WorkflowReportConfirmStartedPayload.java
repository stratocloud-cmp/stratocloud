package com.stratocloud.workflow.messaging;

import com.stratocloud.identity.SimpleUser;

import java.util.List;

public record WorkflowReportConfirmStartedPayload(
        Long nodeInstanceId,
        List<SimpleUser> possibleHandlers) {
}
