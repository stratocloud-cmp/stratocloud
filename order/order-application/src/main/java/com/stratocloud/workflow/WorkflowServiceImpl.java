package com.stratocloud.workflow;

import com.stratocloud.audit.AuditLogContext;
import com.stratocloud.audit.AuditObject;
import com.stratocloud.exceptions.BadCommandException;
import com.stratocloud.exceptions.InvalidWorkflowException;
import com.stratocloud.job.JobDefinition;
import com.stratocloud.repository.JobDefinitionRepository;
import com.stratocloud.repository.WorkflowRepository;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import com.stratocloud.workflow.cmd.*;
import com.stratocloud.workflow.query.DescribeNodeTypesRequest;
import com.stratocloud.workflow.query.DescribeNodeTypesResponse;
import com.stratocloud.workflow.query.DescribeWorkflowsRequest;
import com.stratocloud.workflow.query.NestedWorkflowResponse;
import com.stratocloud.workflow.response.CreateWorkflowResponse;
import com.stratocloud.workflow.response.DeleteWorkflowsResponse;
import com.stratocloud.workflow.response.UpdateWorkflowResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class WorkflowServiceImpl implements WorkflowService {

    private final WorkflowRepository repository;

    private final WorkflowAssembler assembler;

    private final JobDefinitionRepository jobDefinitionRepository;


    public WorkflowServiceImpl(WorkflowRepository repository,
                               WorkflowAssembler assembler,
                               JobDefinitionRepository jobDefinitionRepository) {
        this.repository = repository;
        this.assembler = assembler;
        this.jobDefinitionRepository = jobDefinitionRepository;
    }

    @Override
    @Transactional
    public CreateWorkflowResponse createWorkflow(CreateWorkflowCmd cmd) {
        String workflowName = cmd.getWorkflowName();
        List<NestedWorkflowNode> nestedWorkflowNodes = cmd.getNodes();
        List<NestedSequenceFlow> nestedSequenceFlows = cmd.getSequenceFlows();

        Workflow workflow = new Workflow(workflowName);

        List<Node> nodes = createNodes(nestedWorkflowNodes);

        connectNodes(nodes, nestedSequenceFlows);

        workflow.addNodes(nodes);

        workflow.validate();

        workflow = repository.save(workflow);

        AuditLogContext.current().addAuditObject(
                new AuditObject(workflow.getId().toString(), workflow.getName())
        );

        return new CreateWorkflowResponse(workflow.getId());
    }



    @Override
    @Transactional
    public UpdateWorkflowResponse updateWorkflow(UpdateWorkflowCmd cmd) {
        Long workflowId = cmd.getWorkflowId();
        String workflowName = cmd.getWorkflowName();
        List<NestedWorkflowNode> nestedWorkflowNodes = cmd.getNodes();
        List<NestedSequenceFlow> sequenceFlows = cmd.getSequenceFlows();

        Workflow workflow = repository.findWorkflow(workflowId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(workflow.getId().toString(), workflow.getName())
        );

        List<Node> nodes = createNodes(nestedWorkflowNodes);

        connectNodes(nodes, sequenceFlows);

        workflow.setName(workflowName);

        workflow.getNodes().clear();
        workflow.addNodes(nodes);

        workflow.validate();

        workflow = repository.save(workflow);

        validateBuiltInWorkflow(workflow);

        return new UpdateWorkflowResponse();
    }

    private void validateBuiltInWorkflow(Workflow workflow) {
        Optional<JobDefinition> jobDefinition = jobDefinitionRepository.findByDefaultWorkflowId(workflow.getId());

        if(jobDefinition.isPresent()){
            if(!workflow.isJobTypeIncluded(jobDefinition.get().getJobType()))
                throw new InvalidWorkflowException("内置流程需包含原本的任务类型");

            if(!jobDefinition.get().getDefaultWorkflowRequireOrder()) {
                Optional<Node> requireOrderNode = workflow.getNodes().stream().filter(Node::requireOrder).findAny();
                if (requireOrderNode.isPresent())
                    throw new InvalidWorkflowException(
                            "任务类型[%s]未启用工单，因此不支持[%s]节点".formatted(
                                    jobDefinition.get().getJobTypeName(), requireOrderNode.get().getName()
                            )
                    );
            }

            if(workflow.getJobTypeSet().size() > 1)
                throw new InvalidWorkflowException("内置流程不能包含其他类型的任务节点");
        }

    }

    @Override
    @Transactional
    public DeleteWorkflowsResponse deleteWorkflows(DeleteWorkflowsCmd cmd) {
        List<Long> workflowIds = cmd.getWorkflowIds();

        for (Long workflowId : workflowIds)
            deleteWorkflow(workflowId);


        return new DeleteWorkflowsResponse();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NestedWorkflowResponse> describeWorkflows(DescribeWorkflowsRequest request) {

        Page<Workflow> page = repository.page(
                request.getWorkflowIds(),
                request.getIsReplica(),
                request.getSearch(),
                request.getPageable()
        );

        return page.map(assembler::toNestedWorkflowResponse);
    }

    @Override
    public DescribeNodeTypesResponse describeNodeTypes(DescribeNodeTypesRequest request) {
        List<NodeFactory<?>> nodeFactories = NodeFactoryRegistry.getNodeFactories().stream().sorted(
                Comparator.comparingInt(NodeFactory::getIndex)
        ).toList();
        return new DescribeNodeTypesResponse(nodeFactories.stream().map(assembler::toNestedNodeType).toList());
    }

    private void deleteWorkflow(Long workflowId) {
        Workflow workflow = repository.findWorkflow(workflowId);

        AuditLogContext.current().addAuditObject(
                new AuditObject(workflow.getId().toString(), workflow.getName())
        );

        Optional<JobDefinition> jobDefinition = jobDefinitionRepository.findByDefaultWorkflowId(workflowId);
        boolean isBuiltInWorkflow = jobDefinition.isPresent();

        if(isBuiltInWorkflow)
            throw new BadCommandException("内置流程[%s]无法删除".formatted(workflow.getName()));

        repository.delete(workflow);
    }

    private void connectNodes(List<Node> nodes, List<NestedSequenceFlow> nestedSequenceFlows) {
        Map<String, List<Node>> nodeMap = nodes.stream().collect(Collectors.groupingBy(Node::getNodeKey));

        for (NestedSequenceFlow flow : nestedSequenceFlows) {
            Node sourceNode = nodeMap.get(flow.getSourceNodeKey()).get(0);
            Node targetNode = nodeMap.get(flow.getTargetNodeKey()).get(0);
            sourceNode.connectTo(targetNode);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Node> createNodes(List<NestedWorkflowNode> nestedWorkflowNodes) {
        return nestedWorkflowNodes.stream().map(n -> {
            NodeFactory<NodeProperties> nodeFactory
                    = (NodeFactory<NodeProperties>) NodeFactoryRegistry.getNodeFactory(n.getNodeType());
            Class<?> nodePropertiesClass = Utils.getTypeArgumentClass(nodeFactory.getClass(), NodeFactory.class);
            NodeProperties nodeProperties = (NodeProperties) JSON.convert(n.getNodeProperties(), nodePropertiesClass);
            return nodeFactory.createNode(n.getNodeKey(), n.getNodeName(), nodeProperties);
        }).toList();
    }
}
