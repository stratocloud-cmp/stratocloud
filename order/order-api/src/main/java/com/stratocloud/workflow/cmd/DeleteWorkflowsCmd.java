package com.stratocloud.workflow.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

import java.util.List;

@Data
public class DeleteWorkflowsCmd implements ApiCommand {
    private List<Long> workflowIds;
}
