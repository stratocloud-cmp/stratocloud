package com.stratocloud.order;

public enum OrderStatus {
    AWAIT_SUBMIT,

    PENDING,

    CANCELED,

    DENIED,

    REJECTED,

    PROCESS_ROLLED_BACK,

    AWAIT_APPROVAL,

    AWAIT_CONFIRM,

    EXECUTING,

    FINISHED,

    FAILED,
}
