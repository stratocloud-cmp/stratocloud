package com.stratocloud.workflow.factory;


import com.stratocloud.workflow.Node;
import com.stratocloud.workflow.NodeFactory;
import com.stratocloud.workflow.nodes.EndNode;
import org.springframework.stereotype.Component;

@Component
public class EndNodeFactory implements NodeFactory<EndNodeProperties> {
    @Override
    public String getNodeType() {
        return "END_NODE";
    }

    @Override
    public String getNodeTypeName() {
        return "结束节点";
    }

    @Override
    public Node createNode(String nodeKey, String nodeName, EndNodeProperties nodeProperties) {
        EndNode endNode = new EndNode();
        endNode.setNodeKey(nodeKey);
        endNode.setName(nodeName);
        endNode.setNodeType(getNodeType());
        return endNode;
    }

    @Override
    public int getIndex() {
        return 999;
    }
}
