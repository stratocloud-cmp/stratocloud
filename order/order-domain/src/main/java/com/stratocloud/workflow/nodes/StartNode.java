package com.stratocloud.workflow.nodes;

import com.stratocloud.workflow.Node;
import com.stratocloud.workflow.NodeProperties;
import com.stratocloud.workflow.factory.StartNodeProperties;
import com.stratocloud.workflow.runtime.NodeInstance;
import com.stratocloud.workflow.runtime.StartNodeInstance;
import jakarta.persistence.Entity;

import java.util.Map;

@Entity
public class StartNode extends Node {

    @Override
    public NodeInstance createInstance(Map<String, Object> runtimeProperties) {
        return new StartNodeInstance(this);
    }

    @Override
    public NodeProperties getProperties() {
        return new StartNodeProperties();
    }
}
