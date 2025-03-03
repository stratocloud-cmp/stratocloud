package com.stratocloud.job;

import java.util.Map;

public record JobCanceledPayload(Long jobId, String message, Map<String, Object> parameters) {

}
