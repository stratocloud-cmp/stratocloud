package com.stratocloud.job.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

@Data
public class DisableJobTriggerCmd implements ApiCommand {
    private String triggerId;
}
