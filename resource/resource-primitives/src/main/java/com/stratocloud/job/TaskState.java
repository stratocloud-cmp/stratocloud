package com.stratocloud.job;

import java.util.Set;

public enum TaskState {
    AWAIT_START, STARTED, FAILED, CANCELED, DISCARDED, FINISHED;
    public static final Set<TaskState> END_STATES = Set.of(FAILED, CANCELED, DISCARDED, FINISHED);
}
