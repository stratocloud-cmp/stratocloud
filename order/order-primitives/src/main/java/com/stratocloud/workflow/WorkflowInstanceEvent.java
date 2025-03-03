package com.stratocloud.workflow;

public enum WorkflowInstanceEvent {
    START_REQUESTED,
    CANCEL_REQUESTED,
    COMPLETE_NODE_REQUESTED,
    END_NODE_COMPLETED,
    NODE_COMPLETED,
    NODE_FAILED,
    MOVE_REQUESTED,
    NODE_RESTARTED,
    DISCARD_REQUESTED
}
