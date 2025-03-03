package com.stratocloud.order;

import com.stratocloud.fsm.StateMachine;

public class OrderStateMachine {
    private static final StateMachine<OrderStatus, OrderEvent> fsm = new StateMachine<>();

    static {
        fsm.addTransition(OrderStatus.AWAIT_SUBMIT, OrderEvent.SUBMIT_REQUESTED, OrderStatus.PENDING);
        fsm.addTransition(OrderStatus.AWAIT_SUBMIT, OrderEvent.UPDATE_REQUESTED, OrderStatus.AWAIT_SUBMIT);
        fsm.addTransition(OrderStatus.AWAIT_SUBMIT, OrderEvent.CANCEL_REQUESTED, OrderStatus.CANCELED);

        fsm.addTransition(OrderStatus.PENDING, OrderEvent.WORKFLOW_REPORT_APPROVAL_STARTED, OrderStatus.AWAIT_APPROVAL);
        fsm.addTransition(OrderStatus.PENDING, OrderEvent.WORKFLOW_REPORT_JOB_STARTED, OrderStatus.EXECUTING);
        fsm.addTransition(OrderStatus.PENDING, OrderEvent.WORKFLOW_REPORT_CONFIRM_STARTED, OrderStatus.AWAIT_CONFIRM);
        fsm.addTransition(OrderStatus.PENDING, OrderEvent.WORKFLOW_REPORT_WORKFLOW_FAILED, OrderStatus.FAILED);
        fsm.addTransition(OrderStatus.PENDING, OrderEvent.WORKFLOW_REPORT_WORKFLOW_FINISHED, OrderStatus.FINISHED);

        fsm.addTransition(OrderStatus.REJECTED, OrderEvent.SUBMIT_REQUESTED, OrderStatus.PENDING);
        fsm.addTransition(OrderStatus.REJECTED, OrderEvent.UPDATE_REQUESTED, OrderStatus.REJECTED);
        fsm.addTransition(OrderStatus.REJECTED, OrderEvent.CANCEL_REQUESTED, OrderStatus.CANCELED);

        fsm.addTransition(OrderStatus.PROCESS_ROLLED_BACK, OrderEvent.APPROVE_REQUESTED, OrderStatus.PENDING);
        fsm.addTransition(OrderStatus.PROCESS_ROLLED_BACK, OrderEvent.REJECT_REQUESTED, OrderStatus.REJECTED);
        fsm.addTransition(OrderStatus.PROCESS_ROLLED_BACK, OrderEvent.ROLLBACK_REQUESTED, OrderStatus.PENDING);
        fsm.addTransition(OrderStatus.PROCESS_ROLLED_BACK, OrderEvent.DENY_REQUESTED, OrderStatus.DENIED);
        fsm.addTransition(OrderStatus.PROCESS_ROLLED_BACK, OrderEvent.CANCEL_REQUESTED, OrderStatus.CANCELED);
        fsm.addTransition(OrderStatus.PROCESS_ROLLED_BACK, OrderEvent.UPDATE_REQUESTED, OrderStatus.PROCESS_ROLLED_BACK);

        fsm.addTransition(OrderStatus.AWAIT_APPROVAL, OrderEvent.APPROVE_REQUESTED, OrderStatus.PENDING);
        fsm.addTransition(OrderStatus.AWAIT_APPROVAL, OrderEvent.REJECT_REQUESTED, OrderStatus.REJECTED);
        fsm.addTransition(OrderStatus.AWAIT_APPROVAL, OrderEvent.ROLLBACK_REQUESTED, OrderStatus.PENDING);
        fsm.addTransition(OrderStatus.AWAIT_APPROVAL, OrderEvent.DENY_REQUESTED, OrderStatus.DENIED);
        fsm.addTransition(OrderStatus.AWAIT_APPROVAL, OrderEvent.CANCEL_REQUESTED, OrderStatus.CANCELED);
        fsm.addTransition(OrderStatus.AWAIT_APPROVAL, OrderEvent.WORKFLOW_REPORT_APPROVAL_STARTED, OrderStatus.AWAIT_APPROVAL);
        fsm.addTransition(OrderStatus.AWAIT_APPROVAL, OrderEvent.UPDATE_REQUESTED, OrderStatus.AWAIT_APPROVAL);


        fsm.addTransition(OrderStatus.AWAIT_CONFIRM, OrderEvent.CONFIRM_REQUESTED, OrderStatus.PENDING);
        fsm.addTransition(OrderStatus.AWAIT_CONFIRM, OrderEvent.CANCEL_REQUESTED, OrderStatus.CANCELED);
        fsm.addTransition(OrderStatus.AWAIT_CONFIRM, OrderEvent.UPDATE_REQUESTED, OrderStatus.AWAIT_CONFIRM);

        fsm.addTransition(OrderStatus.EXECUTING, OrderEvent.WORKFLOW_REPORT_JOB_FINISHED, OrderStatus.PENDING);
        fsm.addTransition(OrderStatus.EXECUTING, OrderEvent.WORKFLOW_REPORT_JOB_FAILED, OrderStatus.PENDING);
        fsm.addTransition(OrderStatus.EXECUTING, OrderEvent.WORKFLOW_REPORT_WORKFLOW_FAILED, OrderStatus.FAILED);
        fsm.addTransition(OrderStatus.EXECUTING, OrderEvent.WORKFLOW_REPORT_WORKFLOW_FINISHED, OrderStatus.FINISHED);
        fsm.addTransition(OrderStatus.EXECUTING, OrderEvent.CANCEL_REQUESTED, OrderStatus.CANCELED);

        fsm.addTransition(OrderStatus.FAILED, OrderEvent.WORKFLOW_REPORT_JOB_STARTED, OrderStatus.EXECUTING);
    }


    public static StateMachine<OrderStatus, OrderEvent> get(){
        return fsm;
    }
}
