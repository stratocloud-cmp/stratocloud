package com.stratocloud.job.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

@Data
public class UpdateJobTriggerCmd implements ApiCommand {
    private String triggerId;
    private String cronExpression;
    private String description;
}
