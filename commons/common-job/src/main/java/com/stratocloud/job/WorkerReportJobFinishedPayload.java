package com.stratocloud.job;

import java.util.Map;

public record WorkerReportJobFinishedPayload(Long jobId, Map<String, Object> outputVariables) {
}
