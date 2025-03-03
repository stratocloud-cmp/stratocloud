package com.stratocloud.job;

import com.stratocloud.fsm.StateMachine;

public class TaskStateMachine {
    private static final StateMachine<TaskState, TaskEvent> fsm = new StateMachine<>();

    static {
        fsm.addTransition(TaskState.AWAIT_START, TaskEvent.START_REQUESTED, TaskState.STARTED);
        fsm.addTransition(TaskState.AWAIT_START, TaskEvent.CANCEL_REQUESTED, TaskState.CANCELED);
        fsm.addTransition(TaskState.AWAIT_START, TaskEvent.DISCARD_REQUESTED, TaskState.DISCARDED);

        fsm.addTransition(TaskState.STARTED, TaskEvent.HANDLER_REPORT_FINISHED, TaskState.FINISHED);
        fsm.addTransition(TaskState.STARTED, TaskEvent.HANDLER_REPORT_FAILED, TaskState.FAILED);

        fsm.addTransition(TaskState.FAILED, TaskEvent.RETRY_REQUESTED, TaskState.STARTED);

        fsm.addTransition(TaskState.STARTED, TaskEvent.AUTO_RETRY_START_LATER, TaskState.AWAIT_START);
    }

    public static StateMachine<TaskState, TaskEvent> get(){
        return fsm;
    }
}
