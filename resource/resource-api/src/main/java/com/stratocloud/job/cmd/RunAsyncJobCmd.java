package com.stratocloud.job.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

import java.util.Map;

@Data
public class RunAsyncJobCmd implements ApiCommand {
    private String jobType;
    private Map<String, Object> jobParameters;
    private String note;
}
