package com.stratocloud.workflow;

public enum NodeInstanceEvent {
    START_REQUESTED,
    CANCEL_REQUESTED,
    COMPLETE_REQUESTED,

    DISCARD_REQUESTED,
    RESTART_REQUESTED,
    JOB_REPORT_FAILED
}
