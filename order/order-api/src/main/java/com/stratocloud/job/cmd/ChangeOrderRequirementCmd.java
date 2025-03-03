package com.stratocloud.job.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

@Data
public class ChangeOrderRequirementCmd implements ApiCommand {
    private String jobType;
    private Boolean defaultWorkflowRequireOrder;
}
