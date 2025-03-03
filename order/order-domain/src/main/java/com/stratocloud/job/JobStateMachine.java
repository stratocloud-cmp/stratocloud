package com.stratocloud.job;

import com.stratocloud.fsm.StateMachine;

public class JobStateMachine {
    private static final StateMachine<JobStatus, JobEvent> fsm = new StateMachine<>();

    static {
        fsm.addTransition(JobStatus.AWAIT_START, JobEvent.START_REQUESTED, JobStatus.STARTED);
        fsm.addTransition(JobStatus.AWAIT_START, JobEvent.CANCEL_REQUESTED, JobStatus.CANCELED);

        fsm.addTransition(JobStatus.STARTED, JobEvent.WORKER_REPORT_FINISHED, JobStatus.FINISHED);
        fsm.addTransition(JobStatus.STARTED, JobEvent.WORKER_REPORT_FAILURE, JobStatus.FAILED);

        fsm.addTransition(JobStatus.FAILED, JobEvent.RETRY_REQUESTED, JobStatus.STARTED);
    }

    public static StateMachine<JobStatus, JobEvent> get(){
        return fsm;
    }
}
