package com.stratocloud.workflow.cmd;

import com.stratocloud.request.ApiCommand;
import lombok.Data;

import java.util.List;

@Data
public class CreateWorkflowCmd implements ApiCommand {
    private String workflowName;
    private List<NestedWorkflowNode> nodes;
    private List<NestedSequenceFlow> sequenceFlows;
}
