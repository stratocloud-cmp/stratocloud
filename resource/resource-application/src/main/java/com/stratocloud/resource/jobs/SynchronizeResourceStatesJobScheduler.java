package com.stratocloud.resource.jobs;

import com.stratocloud.constant.CronExpressions;
import com.stratocloud.job.JobScheduler;
import com.stratocloud.job.TriggerParameters;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class SynchronizeResourceStatesJobScheduler implements JobScheduler {

    @Override
    public Map<String, Object> createScheduledJobParameters() {
        return new HashMap<>();
    }

    @Override
    public TriggerParameters getTriggerParameters() {
        return new TriggerParameters(
                true,
                "SYNCHRONIZE_RESOURCE_STATES_TRIGGER",
                CronExpressions.EVERY_THIRTY_MINUTES,
                false,
                "每30分钟同步所有云资源状态"
        );
    }
}
