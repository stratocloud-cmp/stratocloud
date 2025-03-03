package com.stratocloud.workflow.runtime;

import com.stratocloud.workflow.NodeInstanceEvent;
import com.stratocloud.workflow.nodes.EndNode;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;


@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EndNodeInstance extends NodeInstance {
    public EndNodeInstance(EndNode node) {
        super(node);
    }

    @Override
    protected void start() {
        super.start();
        onEvent(NodeInstanceEvent.COMPLETE_REQUESTED);
        workflowInstance.onEndNodeCompleted();
    }
}
