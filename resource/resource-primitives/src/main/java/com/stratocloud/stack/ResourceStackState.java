package com.stratocloud.stack;

public enum ResourceStackState {
    NO_STATE,
    CREATING,
    CREATE_ERROR,
    RUNNING,
    SHUTTING_DOWN,
    SHUTDOWN_ERROR,
    SHUTDOWN
}
