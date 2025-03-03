package com.stratocloud.job;

import java.util.HashMap;
import java.util.Map;

public class DefaultJobScheduler implements JobScheduler{
    @Override
    public Map<String, Object> createScheduledJobParameters() {
        return new HashMap<>();
    }

    @Override
    public TriggerParameters getTriggerParameters() {
        return new TriggerParameters(false, null, null, true, null);
    }
}
