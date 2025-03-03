package com.stratocloud.external.resource;

import com.stratocloud.request.JobParameters;

import java.util.Map;

public interface OrderJobGatewayService {
    void createSingleJob(Long jobId,
                         String jobType,
                         JobParameters jobParameters,
                         String note,
                         Map<String, Object> runtimeProperties);

    String getJobTypeById(Long jobId);
}
