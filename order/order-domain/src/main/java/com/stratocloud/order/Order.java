package com.stratocloud.order;

import com.stratocloud.exceptions.StratoException;
import com.stratocloud.external.order.JobHandlerGatewayService;
import com.stratocloud.identity.SimpleUser;
import com.stratocloud.jpa.entities.Controllable;
import com.stratocloud.repository.WorkflowInstanceRepository;
import com.stratocloud.utils.ContextUtil;
import com.stratocloud.workflow.Workflow;
import com.stratocloud.workflow.WorkflowInstanceStatus;
import com.stratocloud.workflow.nodes.JobNode;
import com.stratocloud.workflow.runtime.JobNodeInstance;
import com.stratocloud.workflow.runtime.NodeInstance;
import com.stratocloud.workflow.runtime.RollbackTarget;
import com.stratocloud.workflow.runtime.WorkflowInstance;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Getter
@Setter(AccessLevel.PROTECTED)
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "strato_order")
public class Order extends Controllable {
    @Column(nullable = false)
    private String orderNo;
    @Column(nullable = false)
    private String orderName;
    @Column(columnDefinition = "TEXT")
    private String note;
    @Column
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    @Column(columnDefinition = "TEXT")
    private String userMessage;
    @Column(columnDefinition = "TEXT")
    private String summary;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "order", orphanRemoval = true)
    @BatchSize(size = 50)
    private List<PossibleHandler> possibleHandlers = new ArrayList<>();
    @Column
    private LocalDateTime submittedAt;
    @Column
    private LocalDateTime lastApprovedAt;
    @Column
    private LocalDateTime endedAt;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "order", orphanRemoval = true)
    @BatchSize(size = 50)
    private List<OrderItem> orderItems = new ArrayList<>();
    @OneToOne
    private Workflow workflow;
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private WorkflowInstance workflowInstance;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "order", orphanRemoval = true)
    private List<OrderActionLog> logs = new ArrayList<>();

    public Order(String orderNo, String orderName, String note, Workflow workflow) {
        this.orderNo = orderNo;
        this.orderName = orderName;
        this.note = note;
        this.workflow = workflow;

        this.status = OrderStatus.AWAIT_SUBMIT;
    }

    public void collectSummaryData(){
        if(!isInSubmittedStates())
            return;

        OrderSummaryBuilder summaryBuilder = new OrderSummaryBuilder();

        for (OrderItem orderItem : orderItems) {
            List<String> details = orderItem.collectSummaryData();
            summaryBuilder.addLine(orderItem.getJobNode().getName(), details);
        }

        this.summary = summaryBuilder.build();
    }

    private void attachWorkflowInstance(WorkflowInstance workflowInstance){
        this.workflowInstance = workflowInstance;

        for (OrderItem orderItem : orderItems) {
            NodeInstance nodeInstance = workflowInstance.getNodeInstanceByNodeId(orderItem.getJobNode().getId());

            if(nodeInstance instanceof JobNodeInstance jobNodeInstance){
                orderItem.attachJobNodeInstance(jobNodeInstance);
            } else {
                throw new StratoException("NodeInstance %s is not a job node.".formatted(nodeInstance.getId()));
            }
        }
    }

    private void createWorkflowInstanceAndSave(Map<String, Object> runtimeProperties){
        WorkflowInstance instance = workflow.createInstance(runtimeProperties);

        var workflowInstanceRepository = ContextUtil.getBean(WorkflowInstanceRepository.class);
        instance = workflowInstanceRepository.save(instance);

        attachWorkflowInstance(instance);
    }

    public void createWorkflowInstance(Map<String, Object> runtimeProperties){
        WorkflowInstance instance = workflow.createInstance(runtimeProperties);

        attachWorkflowInstance(instance);
    }

    public void onSubmit(String message, Map<String, Object> runtimeProperties) {
        onEvent(OrderEvent.SUBMIT_REQUESTED);
        if(workflowInstance == null || workflowInstance.getStatus() == WorkflowInstanceStatus.DISCARDED){
            createWorkflowInstanceAndSave(runtimeProperties);
        }
        workflowInstance.start();
        this.userMessage = message;
        this.submittedAt = LocalDateTime.now();

        appendLog(OrderActionLog.of(this, "SUBMIT", "工单已提交", message));
    }

    private void appendLog(OrderActionLog log) {
        logs.add(log);
    }

    public void onUpdateItem(Long itemId, Map<String, Object> parameters){
        onEvent(OrderEvent.UPDATE_REQUESTED);
        OrderItem item = getItem(itemId);
        item.updateParameters(parameters);

        JobHandlerGatewayService jobHandlerGatewayService = ContextUtil.getBean(JobHandlerGatewayService.class);

        if(isInSubmittedStates())
            jobHandlerGatewayService.notifyJobUpdated(item.getJobNodeInstance().getJob());

        collectSummaryData();
    }

    public boolean isInSubmittedStates(){
        return !OrderStateMachine.get().getPossibleEvents(status).contains(OrderEvent.SUBMIT_REQUESTED);
    }

    public void onCancel(String message){
        onEvent(OrderEvent.CANCEL_REQUESTED);
        possibleHandlers.clear();
        userMessage = message;

        if(workflowInstance != null)
            workflowInstance.cancel(message);

        appendLog(OrderActionLog.of(
                this, "CANCEL", "工单已取消", message
        ));
    }

    public void onReject(String message){
        onEvent(OrderEvent.REJECT_REQUESTED);
        possibleHandlers.clear();
        userMessage = message;
        workflowInstance.discard();
        detachWorkflowInstance();

        appendLog(OrderActionLog.of(
                this, "REJECT", "驳回至申请人", message
        ));
    }

    private void detachWorkflowInstance() {
        orderItems.forEach(OrderItem::detachNodeInstance);
    }

    private Set<Long> getNodeInstanceIdsByHandlerId(Long handlerId){
        return possibleHandlers.stream().filter(
                p->handlerId.equals(p.getUserId())
        ).map(PossibleHandler::getNodeInstanceId).collect(Collectors.toSet());
    }

    public List<String> getNodeNamesByHandlerId(Long handlerId){
        Set<Long> nodeInstanceIds = getNodeInstanceIdsByHandlerId(handlerId);
        List<String> result = new ArrayList<>();
        for (Long nodeInstanceId : nodeInstanceIds) {
            NodeInstance nodeInstance = workflowInstance.getNodeInstanceById(nodeInstanceId);
            String name = nodeInstance.getNode().getName();
            result.add(name);
        }
        return result;
    }

    public void onApprove(Long handlerId, String message){
        onEvent(OrderEvent.APPROVE_REQUESTED);

        userMessage = message;
        Set<Long> nodeInstanceIds = getNodeInstanceIdsByHandlerId(handlerId);
        if(nodeInstanceIds.isEmpty())
            throw new StratoException("User by id %s is not a possible handler.");

        nodeInstanceIds.forEach(workflowInstance::completeNode);
        nodeInstanceIds.forEach(this::removePossibleHandler);


        appendLog(OrderActionLog.of(
                this,
                "APPROVE",
                "审批通过(节点名称: %s)".formatted(getNodeNamesByHandlerId(handlerId)),
                message
        ));
    }

    public void onConfirm(Long handlerId, String message){
        onEvent(OrderEvent.CONFIRM_REQUESTED);

        userMessage = message;
        Set<Long> nodeInstanceIds = getNodeInstanceIdsByHandlerId(handlerId);
        if(nodeInstanceIds.isEmpty())
            throw new StratoException("User by id %s is not a possible handler.");

        nodeInstanceIds.forEach(workflowInstance::completeNode);
        nodeInstanceIds.forEach(this::removePossibleHandler);

        appendLog(OrderActionLog.of(
                this,
                "CONFIRM",
                "用户确认(节点名称: %s)".formatted(getNodeNamesByHandlerId(handlerId)),
                message
        ));
    }

    private void removePossibleHandler(Long nodeInstanceId) {
        possibleHandlers.removeIf(h->Objects.equals(nodeInstanceId, h.getNodeInstanceId()));
    }

    public void onRollback(Long nodeId, String message){
        onEvent(OrderEvent.ROLLBACK_REQUESTED);
        possibleHandlers.clear();
        userMessage = message;
        workflowInstance.moveTo(nodeId);

        appendLog(OrderActionLog.of(
                this,
                "ROLLBACK",
                "退回至[%s]节点".formatted(workflow.getNodeById(nodeId).getName()),
                message
        ));
    }

    public void onDeny(String message){
        onEvent(OrderEvent.DENY_REQUESTED);
        possibleHandlers.clear();
        userMessage = message;
        this.lastApprovedAt = LocalDateTime.now();
        workflowInstance.discard();

        appendLog(OrderActionLog.of(
                this, "DENY", "工单已拒绝", message
        ));
    }

    private void onEvent(OrderEvent event){
        OrderStatus nextState = OrderStateMachine.get().getNextState(status, event);

        if(nextState == null)
            return;

        status = nextState;

        if(OrderEvent.END_EVENTS.contains(event))
            this.endedAt = LocalDateTime.now();

        if(OrderEvent.APPROVAL_EVENTS.contains(event))
            this.lastApprovedAt = LocalDateTime.now();
    }


    public void onFinished() {
        onEvent(OrderEvent.WORKFLOW_REPORT_WORKFLOW_FINISHED);
    }


    public void onFailed(String errorMessage) {
        this.errorMessage = errorMessage;
        onEvent(OrderEvent.WORKFLOW_REPORT_WORKFLOW_FAILED);
    }

    public void onApprovalStarted(Long nodeInstanceId,
                                  List<SimpleUser> possibleHandlers) {
        onEvent(OrderEvent.WORKFLOW_REPORT_APPROVAL_STARTED);

        for (SimpleUser user : possibleHandlers) {
            PossibleHandler possibleHandler = new PossibleHandler(user.userId(), user.realName(), nodeInstanceId);
            addPossibleHandler(possibleHandler);
        }
    }

    private void addPossibleHandler(PossibleHandler possibleHandler) {
        possibleHandler.setOrder(this);
        this.possibleHandlers.add(possibleHandler);
    }

    public void onJobStarted() {
        onEvent(OrderEvent.WORKFLOW_REPORT_JOB_STARTED);
        possibleHandlers.clear();
    }

    public void onConfirmStarted(Long nodeInstanceId, List<SimpleUser> possibleHandlers) {
        onEvent(OrderEvent.WORKFLOW_REPORT_CONFIRM_STARTED);

        for (SimpleUser user : possibleHandlers) {
            PossibleHandler possibleHandler = new PossibleHandler(user.userId(), user.realName(), nodeInstanceId);
            addPossibleHandler(possibleHandler);
        }
    }

    public void addItem(JobNode jobNode, Map<String, Object> parameters) {
        OrderItem orderItem = new OrderItem(this, jobNode, parameters);
        orderItems.add(orderItem);
    }

    public OrderItem getItem(Long orderItemId) {
        return orderItems.stream().filter(
                i -> i.getId().equals(orderItemId)
        ).findAny().orElseThrow(() -> new StratoException("OrderItem %s not found.".formatted(orderItemId)));
    }

    public boolean hasPossibleHandler(Long userId) {
        return possibleHandlers.stream().anyMatch(ph -> Objects.equals(userId, ph.getUserId()));
    }

    public List<RollbackTarget> getRollbackTargets() {
        List<RollbackTarget> rollbackTargets = new ArrayList<>();

        if(workflowInstance == null)
            return rollbackTargets;

        for (NodeInstance nodeInstance : workflowInstance.getNodeInstances())
            if(nodeInstance.isFinished() && nodeInstance instanceof RollbackTarget rollbackTarget)
                rollbackTargets.add(rollbackTarget);


        return rollbackTargets;
    }
}
