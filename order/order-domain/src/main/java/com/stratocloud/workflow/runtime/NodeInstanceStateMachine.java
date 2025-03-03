package com.stratocloud.workflow.runtime;

import com.stratocloud.fsm.StateMachine;
import com.stratocloud.workflow.NodeInstanceEvent;
import com.stratocloud.workflow.NodeInstanceStatus;

public class NodeInstanceStateMachine {
    private static final StateMachine<NodeInstanceStatus, NodeInstanceEvent> fsm = new StateMachine<>();

    static {
        fsm.addTransition(NodeInstanceStatus.AWAIT_START, NodeInstanceEvent.START_REQUESTED, NodeInstanceStatus.STARTED);
        fsm.addTransition(NodeInstanceStatus.AWAIT_START, NodeInstanceEvent.CANCEL_REQUESTED, NodeInstanceStatus.CANCELED);
        fsm.addTransition(NodeInstanceStatus.AWAIT_START, NodeInstanceEvent.DISCARD_REQUESTED, NodeInstanceStatus.DISCARDED);

        fsm.addTransition(NodeInstanceStatus.STARTED, NodeInstanceEvent.COMPLETE_REQUESTED, NodeInstanceStatus.FINISHED);
        fsm.addTransition(NodeInstanceStatus.STARTED, NodeInstanceEvent.JOB_REPORT_FAILED, NodeInstanceStatus.FAILED);

        fsm.addTransition(NodeInstanceStatus.FAILED, NodeInstanceEvent.RESTART_REQUESTED, NodeInstanceStatus.STARTED);
    }

    public static StateMachine<NodeInstanceStatus, NodeInstanceEvent> get(){
        return fsm;
    }
}
