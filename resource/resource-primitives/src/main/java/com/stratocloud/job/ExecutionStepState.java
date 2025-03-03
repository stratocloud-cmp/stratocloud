package com.stratocloud.job;

public enum ExecutionStepState {
    AWAIT_START,
    STARTED,
    FAILED,
    DISCARDED, CANCELED, FINISHED
}
