package com.stratocloud.workflow;


import com.stratocloud.utils.Utils;

public interface NodeFactory<P extends NodeProperties> {
    String getNodeType();

    String getNodeTypeName();

    Node createNode(String nodeKey, String nodeName, P nodeProperties);

    default int getIndex(){
        return 1;
    }

    @SuppressWarnings("unchecked")
    default Class<P> getNodePropertiesClass(){
        return (Class<P>) Utils.getTypeArgumentClass(getClass(), NodeFactory.class);
    }
}
