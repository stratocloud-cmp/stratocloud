package com.stratocloud.workflow.runtime;

import com.stratocloud.workflow.nodes.StartNode;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StartNodeInstance extends NodeInstance {
    public StartNodeInstance(StartNode node) {
        super(node);
    }

    @Override
    protected void start() {
        super.start();
        super.complete();
    }
}
