package com.stratocloud.job.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

import java.util.Map;

@Data
public class CreateJobCmd implements ApiCommand {
    private Long jobId;
    private String jobType;
    private Map<String, Object> jobParameters;
    private Map<String, Object> runtimeProperties;
    private String note;
}
