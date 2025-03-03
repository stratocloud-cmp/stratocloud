package com.stratocloud.workflow.factory;

import com.stratocloud.workflow.Node;
import com.stratocloud.workflow.NodeFactory;
import com.stratocloud.workflow.nodes.StartNode;
import org.springframework.stereotype.Component;

@Component
public class StartNodeFactory implements NodeFactory<StartNodeProperties> {
    @Override
    public String getNodeType() {
        return "START_NODE";
    }

    @Override
    public String getNodeTypeName() {
        return "开始节点";
    }

    @Override
    public Node createNode(String nodeKey, String nodeName, StartNodeProperties nodeProperties) {
        StartNode startNode = new StartNode();
        startNode.setNodeKey(nodeKey);
        startNode.setNodeType(getNodeType());
        startNode.setName(nodeName);
        return startNode;
    }

    @Override
    public int getIndex() {
        return 0;
    }
}
