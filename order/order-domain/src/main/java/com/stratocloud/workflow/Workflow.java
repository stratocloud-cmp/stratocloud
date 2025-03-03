package com.stratocloud.workflow;

import com.stratocloud.exceptions.InvalidWorkflowException;
import com.stratocloud.exceptions.StratoException;
import com.stratocloud.job.JobDefinition;
import com.stratocloud.jpa.entities.Tenanted;
import com.stratocloud.utils.Assert;
import com.stratocloud.utils.GraphUtil;
import com.stratocloud.utils.Utils;
import com.stratocloud.workflow.nodes.EndNode;
import com.stratocloud.workflow.nodes.JobNode;
import com.stratocloud.workflow.nodes.StartNode;
import com.stratocloud.workflow.runtime.WorkflowInstance;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Entity
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Workflow extends Tenanted {
    @Column
    private String name;
    @Column
    private Boolean isReplica = false;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "workflow", orphanRemoval = true)
    private List<Node> nodes = new ArrayList<>();

    public Workflow(String name) {
        this.name = name;
    }

    public static Workflow createSingleJobWorkflow(JobDefinition jobDefinition) {
        StartNode startNode = new StartNode();
        startNode.setName("开始节点");
        startNode.setNodeKey("StartNode");
        startNode.setNodeType("START_NODE");

        JobNode jobNode = new JobNode(jobDefinition);
        jobNode.setName("任务节点");
        jobNode.setNodeKey("JobNode");
        jobNode.setNodeType("JOB_NODE");


        EndNode endNode = new EndNode();
        endNode.setName("结束节点");
        endNode.setNodeKey("EndNode");
        endNode.setNodeType("END_NODE");

        startNode.connectTo(jobNode);
        jobNode.connectTo(endNode);

        Workflow workflow = new Workflow(jobDefinition.getJobTypeName());

        workflow.addNodes(List.of(startNode, jobNode, endNode));

        return workflow;
    }

    public void addNode(Node node){
        node.setWorkflow(this);
        this.nodes.add(node);
    }

    public void addNodes(Collection<Node> nodes){
        if(nodes == null)
            return;

        nodes.forEach(this::addNode);
    }

    public void validate(){
        Assert.isNotBlank(name, "流程名称不能为空");

        if(Utils.isEmpty(nodes))
            throw new InvalidWorkflowException("流程中没有流程节点");

        StartNode startNode = getStartNode();
        EndNode endNode = getEndNode();

        List<Node> reachableNodeList = GraphUtil.bfs(startNode, Node::getToNodes);

        if(reachableNodeList.stream().noneMatch(node -> Objects.equals(node.getNodeKey(), endNode.getNodeKey())))
            throw new InvalidWorkflowException("结束节点不可达");

        Set<String> nodeKeySet = nodes.stream().map(Node::getNodeKey).collect(Collectors.toSet());

        if(nodeKeySet.size() != nodes.size())
            throw new InvalidWorkflowException("流程中存在重复的节点标识");

        nodes.forEach(Node::validate);
    }

    public WorkflowInstance createInstance(Map<String, Object> runtimeProperties){
        WorkflowInstance workflowInstance = new WorkflowInstance(this, runtimeProperties);
        workflowInstance.validate();
        return workflowInstance;
    }


    @SuppressWarnings("unchecked")
    public <T extends Node> List<T> getNodesByType(Class<T> nodeClass) {
        return (List<T>) nodes.stream().filter(node -> nodeClass.isAssignableFrom(node.getClass())).toList();
    }

    public Node getNodeById(Long nodeId) {
        return nodes.stream().filter(
                n -> n.getId().equals(nodeId)
        ).findAny().orElseThrow(
                () -> new StratoException("NodeId not found in workflow %s".formatted(name))
        );
    }

    @SuppressWarnings("unchecked")
    public Workflow createReplica(){
        Workflow replica = new Workflow(name);
        replica.setIsReplica(true);

        for (Node node : nodes) {
            NodeFactory<NodeProperties> nodeFactory
                    = (NodeFactory<NodeProperties>) NodeFactoryRegistry.getNodeFactory(node.getNodeType());

            NodeProperties nodeProperties = node.getProperties();
            Node replicaNode = nodeFactory.createNode(node.getNodeKey(), node.getName(), nodeProperties);
            replica.addNode(replicaNode);
        }

        Set<Node> visitedSet = new HashSet<>();
        StartNode startNode = getStartNode();
        Queue<Node> queue = new LinkedList<>();
        queue.offer(startNode);

        while (!queue.isEmpty()){
            Node fromNode = queue.poll();

            if(visitedSet.contains(fromNode))
                continue;
            else
                visitedSet.add(fromNode);

            for (Node toNode : fromNode.getToNodes()) {
                Node fromNodeReplica = replica.getNodeByKey(fromNode.getNodeKey());
                Node toNodeReplica = replica.getNodeByKey(toNode.getNodeKey());
                fromNodeReplica.connectTo(toNodeReplica);
                queue.offer(toNode);
            }
        }

        return replica;
    }

    private Node getNodeByKey(String nodeKey) {
        return nodes.stream().filter(
                n->Objects.equals(n.getNodeKey(), nodeKey)
        ).findAny().orElseThrow(()->new InvalidWorkflowException("流程中没有key值为%s的节点".formatted(nodeKey)));
    }

    public StartNode getStartNode() {
        List<StartNode> startNodes = getNodesByType(StartNode.class);

        if(startNodes.isEmpty())
            throw new InvalidWorkflowException("流程中缺少开始节点");
        if(startNodes.size()>1)
            throw new InvalidWorkflowException("流程不得有多个开始节点");

        return startNodes.get(0);
    }

    public EndNode getEndNode() {
        List<EndNode> endNodes = getNodesByType(EndNode.class);

        if(endNodes.isEmpty())
            throw new InvalidWorkflowException("流程中缺少结束节点");

        return endNodes.get(0);
    }

    public boolean isJobTypeIncluded(String jobType) {
        List<JobNode> jobNodes = getNodesByType(JobNode.class);
        return jobNodes.stream().anyMatch(
                jobNode -> Objects.equals(
                        jobType,
                        jobNode.getJobDefinition().getJobType()
                )
        );
    }

    public Set<String> getJobTypeSet(){
        return getNodesByType(JobNode.class).stream().map(
                node -> node.getJobDefinition().getJobType()
        ).collect(Collectors.toSet());
    }
}
