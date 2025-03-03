package com.stratocloud.workflow.factory;

import com.stratocloud.identity.SimpleUser;
import com.stratocloud.external.order.UserGatewayService;
import com.stratocloud.utils.Assert;
import com.stratocloud.workflow.Node;
import com.stratocloud.workflow.NodeFactory;
import com.stratocloud.workflow.nodes.ConfirmNode;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ConfirmNodeFactory implements NodeFactory<ConfirmNodeProperties> {

    private final UserGatewayService userGatewayService;

    public ConfirmNodeFactory(UserGatewayService userGatewayService) {
        this.userGatewayService = userGatewayService;
    }

    @Override
    public String getNodeType() {
        return "CONFIRM_NODE";
    }

    @Override
    public String getNodeTypeName() {
        return "确认节点";
    }

    @Override
    public Node createNode(String nodeKey, String nodeName, ConfirmNodeProperties nodeProperties) {
        Assert.isNotEmpty(nodeProperties.getConfirmHandlerIds(), "节点[%s]未选择处理人".formatted(nodeName));

        List<SimpleUser> users = userGatewayService.findUsers(nodeProperties.getConfirmHandlerIds());
        ConfirmNode confirmNode = new ConfirmNode(users);
        confirmNode.setNodeKey(nodeKey);
        confirmNode.setName(nodeName);
        confirmNode.setNodeType(getNodeType());
        return confirmNode;
    }
}
