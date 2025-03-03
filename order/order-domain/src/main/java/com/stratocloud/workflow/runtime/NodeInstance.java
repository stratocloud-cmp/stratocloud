package com.stratocloud.workflow.runtime;

import com.stratocloud.jpa.entities.Auditable;
import com.stratocloud.workflow.Node;
import com.stratocloud.workflow.NodeInstanceEvent;
import com.stratocloud.workflow.NodeInstanceStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter(value = AccessLevel.PROTECTED)
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class NodeInstance extends Auditable {
    @Column
    @Enumerated(EnumType.STRING)
    private NodeInstanceStatus status;

    @ManyToOne
    protected WorkflowInstance workflowInstance;
    @ManyToOne
    private Node node;

    public NodeInstance(Node node) {
        this.node = node;
        this.status = NodeInstanceStatus.AWAIT_START;
    }

    protected void cancel(String message) {
        onEvent(NodeInstanceEvent.CANCEL_REQUESTED);
    }

    protected void discard(){
        onEvent(NodeInstanceEvent.DISCARD_REQUESTED);
    }

    public void complete() {
        onEvent(NodeInstanceEvent.COMPLETE_REQUESTED);

        workflowInstance.onNodeCompleted(this);
    }

    public void onFailed(String errorMessage){
        onEvent(NodeInstanceEvent.JOB_REPORT_FAILED);

        workflowInstance.onNodeFailed(errorMessage);
    }

    protected List<NodeInstance> getFromNodeInstances() {
        return node.getFromNodes().stream().map(n->workflowInstance.getNodeInstanceByNodeId(n.getId())).toList();
    }

    protected List<NodeInstance> getToNodeInstances() {
        return node.getToNodes().stream().map(n->workflowInstance.getNodeInstanceByNodeId(n.getId())).toList();
    }

    protected void start(){
        onEvent(NodeInstanceEvent.START_REQUESTED);
    }

    protected void onEvent(NodeInstanceEvent event){
        NodeInstanceStatus nextState = NodeInstanceStateMachine.get().getNextState(status, event);

        if(nextState == null)
            return;

        status = nextState;
    }


    @Override
    public String toString() {
        return "NodeInstance{" +
                "status=" + status +
                ", class="+ getClass().getSimpleName() +
                '}';
    }

    public boolean isFinished() {
        return status == NodeInstanceStatus.FINISHED;
    }
}
