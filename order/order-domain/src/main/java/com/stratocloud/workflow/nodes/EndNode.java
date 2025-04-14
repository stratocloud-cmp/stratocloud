package com.stratocloud.workflow.nodes;

import com.stratocloud.workflow.Node;
import com.stratocloud.workflow.NodeProperties;
import com.stratocloud.workflow.factory.EndNodeProperties;
import com.stratocloud.workflow.runtime.EndNodeInstance;
import com.stratocloud.workflow.runtime.NodeInstance;
import jakarta.persistence.Entity;

import java.util.Map;

@Entity
public class EndNode extends Node {
    @Override
    public NodeInstance createInstance(Map<String, Object> runtimeProperties) {
        return new EndNodeInstance(this);
    }

    @Override
    public NodeProperties getProperties() {
        return new EndNodeProperties();
    }
}
