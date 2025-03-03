package com.stratocloud.job;

import java.util.List;

public record WorkerReportJobFailedPayload(Long jobId, List<String> errorMessages) {
}
