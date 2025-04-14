package com.stratocloud.external.order;

import com.stratocloud.job.Job;

import java.util.List;
import java.util.Map;

public interface JobHandlerGatewayService {
    void notifyJobUpdated(Job job);

    Map<String, Object> createScheduledJobParameters(String jobType);

    List<String> collectSummaryData(Job job);

    void preCreateJob(Job job);

    Map<String,Object> prepareRuntimeProperties(String jobType, Map<String, Object> parameters);
}
