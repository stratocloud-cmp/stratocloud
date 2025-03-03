package com.stratocloud.workflow.runtime;

import com.stratocloud.exceptions.InvalidWorkflowException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.jpa.entities.Controllable;
import com.stratocloud.messaging.Message;
import com.stratocloud.utils.GraphUtil;
import com.stratocloud.workflow.*;
import com.stratocloud.workflow.messaging.WorkflowReportWorkflowFailedPayload;
import com.stratocloud.workflow.messaging.WorkflowReportWorkflowFinishedPayload;
import com.stratocloud.workflow.messaging.WorkflowTopics;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.*;

@Getter
@Setter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkflowInstance extends Controllable {
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Workflow workflow;
    @Column
    @Enumerated(EnumType.STRING)
    private WorkflowInstanceStatus status;
    @Column
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> runtimeProperties = new HashMap<>();
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "workflowInstance", orphanRemoval = true)
    private List<NodeInstance> nodeInstances = new ArrayList<>();

    public WorkflowInstance(Workflow workflow,
                            Map<String, Object> runtimeProperties) {
        this.workflow = workflow;
        if(runtimeProperties!=null)
            this.runtimeProperties.putAll(runtimeProperties);
        this.status = WorkflowInstanceStatus.AWAIT_START;
        initNodeInstances();
    }

    private void initNodeInstances() {
        for (Node node : workflow.getNodes()) {
            NodeInstance nodeInstance = node.createInstance();
            addNodeInstance(nodeInstance);
        }
    }

    public void validate(){
        validateStartNodeInstance();
        validateEndNodeInstance();
    }

    public NodeInstance getNodeInstanceByNodeId(Long nodeId){
        return nodeInstances.stream().filter(i -> Objects.equals(nodeId, i.getNode().getId())).findAny()
                .orElseThrow(()->new StratoException("NodeInstance not found by nodeId: %s".formatted(nodeId)));
    }

    public List<NodeInstance> getNodeInstancesByStatus(Set<NodeInstanceStatus> statuses){
        return nodeInstances.stream().filter(n -> statuses.contains(n.getStatus())).toList();
    }

    public List<NodeInstance> getNodeInstancesByType(Class<? extends NodeInstance> clazz){
        return nodeInstances.stream().filter(n -> clazz.equals(n.getClass())).toList();
    }

    private NodeInstance getStartNodeInstance(){
        List<NodeInstance> list = getNodeInstancesByType(StartNodeInstance.class);

        if(list.isEmpty())
            throw new InvalidWorkflowException("流程中缺少开始节点");
        if(list.size()>1)
            throw new InvalidWorkflowException("流程不得有多个开始节点");

        return list.get(0);
    }

    private void validateStartNodeInstance(){
        getStartNodeInstance();
    }

    private void validateEndNodeInstance(){
        List<NodeInstance> list = getNodeInstancesByType(EndNodeInstance.class);

        if(list.isEmpty())
            throw new InvalidWorkflowException("流程中缺少结束节点");
        if(list.size()>1)
            throw new InvalidWorkflowException("流程不得有多个结束节点");
    }

    public void addNodeInstance(NodeInstance nodeInstance){
        nodeInstance.setWorkflowInstance(this);
        this.nodeInstances.add(nodeInstance);
    }

    public void start(){
        onEvent(WorkflowInstanceEvent.START_REQUESTED);

        NodeInstance nodeInstance = getStartNodeInstance();
        nodeInstance.start();
    }

    public void cancel(String message) {
        onEvent(WorkflowInstanceEvent.CANCEL_REQUESTED);

        List<NodeInstance> instancesToCancel = getNodeInstancesByStatus(Set.of(NodeInstanceStatus.AWAIT_START));
        instancesToCancel.forEach(n -> n.cancel(message));
    }

    public void discard() {
        onEvent(WorkflowInstanceEvent.DISCARD_REQUESTED);

        List<NodeInstance> instancesToDiscard = getNodeInstancesByStatus(Set.of(NodeInstanceStatus.AWAIT_START));
        instancesToDiscard.forEach(NodeInstance::discard);
    }

    public void completeNode(Long nodeInstanceId) {
        onEvent(WorkflowInstanceEvent.COMPLETE_NODE_REQUESTED);

        NodeInstance nodeInstance = getNodeInstanceById(nodeInstanceId);
        nodeInstance.complete();
    }

    public NodeInstance getNodeInstanceById(Long nodeInstanceId) {
        return nodeInstances.stream().filter(
                n->n.getId().equals(nodeInstanceId)
        ).findAny().orElseThrow(()->new StratoException("NodeInstance not found by id: %s".formatted(nodeInstanceId)));
    }

    public void onNodeCompleted(NodeInstance nodeInstance){
        onEvent(WorkflowInstanceEvent.NODE_COMPLETED);

        List<NodeInstance> toNodeInstances = nodeInstance.getToNodeInstances();

        toNodeInstances.forEach(NodeInstance::start);
    }

    public void onNodeFailed(String errorMessage){
        onEvent(WorkflowInstanceEvent.NODE_FAILED);

        Message message = Message.create(
                WorkflowTopics.WORKFLOW_REPORT_WORKFLOW_FAILED,
                new WorkflowReportWorkflowFailedPayload(getId(), errorMessage),
                getId().toString()
        );

        publish(message);
    }

    public void onEndNodeCompleted(){
        onEvent(WorkflowInstanceEvent.END_NODE_COMPLETED);

        Message message = Message.create(
                WorkflowTopics.WORKFLOW_REPORT_WORKFLOW_FINISHED,
                new WorkflowReportWorkflowFinishedPayload(getId()),
                getId().toString()
        );

        publish(message);
    }



    public void moveTo(Long nodeId) {
        resetNodeInstances();

        NodeInstance nodeInstance = getNodeInstanceByNodeId(nodeId);

        List<NodeInstance> nodeInstancesToSkip = GraphUtil.bfs(nodeInstance, NodeInstance::getFromNodeInstances);
        nodeInstancesToSkip.remove(nodeInstance);

        nodeInstancesToSkip.forEach(n->n.setStatus(NodeInstanceStatus.FINISHED));
        nodeInstance.start();

        onEvent(WorkflowInstanceEvent.MOVE_REQUESTED);
    }

    private void resetNodeInstances(){
        nodeInstances.forEach(n->n.setStatus(NodeInstanceStatus.AWAIT_START));
    }


    private void onEvent(WorkflowInstanceEvent event){
        WorkflowInstanceStatus nextState = WorkflowInstanceStateMachine.get().getNextState(status, event);

        if(nextState == null)
            return;

        status = nextState;
    }

    public void addRuntimeProperties(Map<String, Object> outputVariables) {
        if(outputVariables == null)
            return;

        runtimeProperties.putAll(outputVariables);
    }


    @Override
    public String toString() {
        return "WorkflowInstance{" +
                "status=" + status +
                ", nodeInstances=" + nodeInstances +
                '}';
    }

    public void onNodeRestarted() {
        onEvent(WorkflowInstanceEvent.NODE_RESTARTED);
    }
}
