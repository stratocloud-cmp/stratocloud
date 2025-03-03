package com.stratocloud.job.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

@Data
public class TriggerJobOnceCmd implements ApiCommand {
    private String triggerId;
}
