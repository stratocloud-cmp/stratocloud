package com.stratocloud.job;


import java.util.Map;

public interface JobScheduler {
    Map<String, Object> createScheduledJobParameters();
    TriggerParameters getTriggerParameters();
}
