package com.stratocloud.workflow;

import com.stratocloud.exceptions.StratoException;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class NodeFactoryRegistry {
    private static final Map<String, NodeFactory<?>> nodeFactoryMap = new ConcurrentHashMap<>();

    public static void register(NodeFactory<?> nodeFactory){
        if(nodeFactory == null)
            return;

        nodeFactoryMap.put(nodeFactory.getNodeType(), nodeFactory);
        log.info("Node factory {} registered.", nodeFactory.getNodeType());
    }

    public static NodeFactory<?> getNodeFactory(String nodeType){
        NodeFactory<?> nodeFactory = nodeFactoryMap.get(nodeType);

        if(nodeFactory == null)
            throw new StratoException("NodeFactory for nodeType %s not found".formatted(nodeType));

        return nodeFactory;
    }

    public static List<NodeFactory<?>> getNodeFactories() {
        return new ArrayList<>(nodeFactoryMap.values());
    }
}
