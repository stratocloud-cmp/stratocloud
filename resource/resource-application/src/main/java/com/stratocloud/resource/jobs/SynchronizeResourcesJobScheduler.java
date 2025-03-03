package com.stratocloud.resource.jobs;

import com.stratocloud.constant.CronExpressions;
import com.stratocloud.job.JobScheduler;
import com.stratocloud.job.TriggerParameters;
import com.stratocloud.resource.cmd.SynchronizeResourcesCmd;
import com.stratocloud.utils.JSON;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SynchronizeResourcesJobScheduler implements JobScheduler {

    @Override
    public Map<String, Object> createScheduledJobParameters() {
        SynchronizeResourcesCmd cmd = new SynchronizeResourcesCmd();
        cmd.setSynchronizeAll(true);
        return JSON.toMap(cmd);
    }

    @Override
    public TriggerParameters getTriggerParameters() {
        return new TriggerParameters(
                true,
                "SYNCHRONIZE_RESOURCES_TRIGGER",
                CronExpressions.EVERY_DAY_MIDNIGHT,
                false,
                "每天凌晨零点同步所有云资源"
        );
    }
}
