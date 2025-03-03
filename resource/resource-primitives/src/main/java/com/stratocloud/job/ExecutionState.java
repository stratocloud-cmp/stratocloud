package com.stratocloud.job;

import java.util.Set;

public enum ExecutionState {
    AWAIT_START,
    STARTED,
    FAILED,
    FINISHED,
    DISCARDED,
    CANCELED;
    public static final Set<ExecutionState> END_STATES = Set.of(FAILED, FINISHED, DISCARDED, CANCELED);
}
