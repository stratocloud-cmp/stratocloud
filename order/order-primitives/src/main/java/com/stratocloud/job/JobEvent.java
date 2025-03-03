package com.stratocloud.job;

public enum JobEvent {
    START_REQUESTED,
    RETRY_REQUESTED,
    CANCEL_REQUESTED,

    WORKER_REPORT_FINISHED,
    WORKER_REPORT_FAILURE
}
