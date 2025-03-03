package com.stratocloud.workflow.nodes;

import com.stratocloud.workflow.Node;
import com.stratocloud.workflow.NodeProperties;
import com.stratocloud.workflow.factory.EndNodeProperties;
import com.stratocloud.workflow.runtime.EndNodeInstance;
import com.stratocloud.workflow.runtime.NodeInstance;
import jakarta.persistence.Entity;

@Entity
public class EndNode extends Node {
    @Override
    public NodeInstance createInstance() {
        return new EndNodeInstance(this);
    }

    @Override
    public NodeProperties getProperties() {
        return new EndNodeProperties();
    }
}
