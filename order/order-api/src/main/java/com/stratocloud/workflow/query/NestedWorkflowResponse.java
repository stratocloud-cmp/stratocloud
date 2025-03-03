package com.stratocloud.workflow.query;

import com.stratocloud.request.query.NestedTenanted;
import com.stratocloud.workflow.cmd.NestedSequenceFlow;
import com.stratocloud.workflow.cmd.NestedWorkflowNode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class NestedWorkflowResponse extends NestedTenanted {
    private String name;
    private Boolean isReplica = false;
    private List<NestedWorkflowNode> nodes;
    private List<NestedSequenceFlow> sequenceFlows;
}
