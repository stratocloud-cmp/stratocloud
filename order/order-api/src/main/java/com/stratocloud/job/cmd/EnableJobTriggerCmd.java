package com.stratocloud.job.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

@Data
public class EnableJobTriggerCmd implements ApiCommand {
    private String triggerId;
}
