package com.stratocloud.workflow.runtime;

import com.stratocloud.fsm.StateMachine;
import com.stratocloud.workflow.WorkflowInstanceEvent;
import com.stratocloud.workflow.WorkflowInstanceStatus;

public class WorkflowInstanceStateMachine {
    private static final StateMachine<WorkflowInstanceStatus, WorkflowInstanceEvent> fsm = new StateMachine<>();

    static {
        fsm.addTransition(
                WorkflowInstanceStatus.AWAIT_START,
                WorkflowInstanceEvent.START_REQUESTED,
                WorkflowInstanceStatus.RUNNING
        );
        fsm.addTransition(
                WorkflowInstanceStatus.AWAIT_START,
                WorkflowInstanceEvent.CANCEL_REQUESTED,
                WorkflowInstanceStatus.CANCELED
        );
        fsm.addTransition(
                WorkflowInstanceStatus.AWAIT_START,
                WorkflowInstanceEvent.DISCARD_REQUESTED,
                WorkflowInstanceStatus.DISCARDED
        );


        fsm.addTransition(
                WorkflowInstanceStatus.RUNNING,
                WorkflowInstanceEvent.DISCARD_REQUESTED,
                WorkflowInstanceStatus.DISCARDED
        );
        fsm.addTransition(
                WorkflowInstanceStatus.RUNNING,
                WorkflowInstanceEvent.CANCEL_REQUESTED,
                WorkflowInstanceStatus.CANCELED
        );
        fsm.addTransition(
                WorkflowInstanceStatus.RUNNING,
                WorkflowInstanceEvent.COMPLETE_NODE_REQUESTED
        );
        fsm.addTransition(
                WorkflowInstanceStatus.RUNNING,
                WorkflowInstanceEvent.MOVE_REQUESTED
        );


        fsm.addTransition(
                WorkflowInstanceStatus.RUNNING,
                WorkflowInstanceEvent.END_NODE_COMPLETED,
                WorkflowInstanceStatus.FINISHED
        );
        fsm.addTransition(
                WorkflowInstanceStatus.RUNNING,
                WorkflowInstanceEvent.NODE_COMPLETED
        );
        fsm.addTransition(
                WorkflowInstanceStatus.RUNNING,
                WorkflowInstanceEvent.NODE_FAILED,
                WorkflowInstanceStatus.FAILED
        );

        fsm.addTransition(
                WorkflowInstanceStatus.FAILED,
                WorkflowInstanceEvent.NODE_RESTARTED,
                WorkflowInstanceStatus.RUNNING
        );
    }

    public static StateMachine<WorkflowInstanceStatus, WorkflowInstanceEvent> get(){
        return fsm;
    }
}
