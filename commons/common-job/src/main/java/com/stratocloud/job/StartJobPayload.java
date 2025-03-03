package com.stratocloud.job;

import java.util.Map;

public record StartJobPayload(Long jobId,
                              Map<String, Object> parameters,
                              Map<String, Object> runtimeVariables) {
}
