package com.stratocloud.workflow;

import com.stratocloud.form.DynamicFormHelper;
import com.stratocloud.jpa.entities.EntityUtil;
import com.stratocloud.utils.GraphUtil;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import com.stratocloud.workflow.cmd.NestedSequenceFlow;
import com.stratocloud.workflow.cmd.NestedWorkflowNode;
import com.stratocloud.workflow.nodes.StartNode;
import com.stratocloud.workflow.query.NestedNodeType;
import com.stratocloud.workflow.query.NestedWorkflowResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class WorkflowAssembler {
    public NestedWorkflowResponse toNestedWorkflowResponse(Workflow workflow) {
        NestedWorkflowResponse response = new NestedWorkflowResponse();

        EntityUtil.copyBasicFields(workflow, response);

        response.setName(workflow.getName());
        response.setIsReplica(workflow.getIsReplica());
        response.setNodes(toNestedWorkflowNodes(workflow.getNodes()));
        response.setSequenceFlows(toNestedSequenceFlows(workflow));
        return response;
    }

    private List<NestedSequenceFlow> toNestedSequenceFlows(Workflow workflow) {
        Map<Long, SequenceFlow> flowMap = new HashMap<>();

        StartNode startNode = workflow.getStartNode();

        List<SequenceFlow> outgoingFlows = startNode.getOutgoingFlows();
        for (SequenceFlow outgoingFlow : outgoingFlows) {
            List<SequenceFlow> flowList
                    = GraphUtil.bfs(outgoingFlow, sequenceFlow -> sequenceFlow.getTarget().getOutgoingFlows());

            flowList.forEach(sequenceFlow -> flowMap.put(sequenceFlow.getId(), sequenceFlow));
        }

        return flowMap.values().stream().map(this::toNestedSequenceFlow).toList();
    }

    private NestedSequenceFlow toNestedSequenceFlow(SequenceFlow sequenceFlow) {
        NestedSequenceFlow nestedSequenceFlow = new NestedSequenceFlow();
        nestedSequenceFlow.setSourceNodeKey(sequenceFlow.getSource().getNodeKey());
        nestedSequenceFlow.setTargetNodeKey(sequenceFlow.getTarget().getNodeKey());
        return nestedSequenceFlow;

    }

    private List<NestedWorkflowNode> toNestedWorkflowNodes(List<Node> nodes) {
        List<NestedWorkflowNode> result = new ArrayList<>();
        if(Utils.isEmpty(nodes))
            return result;

        for (Node node : nodes) {
            result.add(toNestedWorkflowNode(node));
        }

        return result;
    }

    private NestedWorkflowNode toNestedWorkflowNode(Node node) {
        NestedWorkflowNode nestedWorkflowNode = new NestedWorkflowNode();

        nestedWorkflowNode.setNodeType(node.getNodeType());
        nestedWorkflowNode.setNodeKey(node.getNodeKey());
        nestedWorkflowNode.setNodeName(node.getName());
        nestedWorkflowNode.setNodeProperties(JSON.toMap(node.getProperties()));

        return nestedWorkflowNode;
    }

    public NestedNodeType toNestedNodeType(NodeFactory<?> nodeFactory) {
        NestedNodeType nestedNodeType = new NestedNodeType();
        nestedNodeType.setNodeType(nodeFactory.getNodeType());
        nestedNodeType.setNodeTypeName(nodeFactory.getNodeTypeName());
        nestedNodeType.setNodePropertiesFormMetaData(
                DynamicFormHelper.generateMetaData(nodeFactory.getNodePropertiesClass())
        );
        return nestedNodeType;
    }
}
