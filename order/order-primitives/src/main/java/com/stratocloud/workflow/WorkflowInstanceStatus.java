package com.stratocloud.workflow;

public enum WorkflowInstanceStatus {
    AWAIT_START,
    RUNNING,
    FINISHED,
    FAILED,
    CANCELED,

    DISCARDED
}
