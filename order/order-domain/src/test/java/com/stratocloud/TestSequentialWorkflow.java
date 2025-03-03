package com.stratocloud;


import com.stratocloud.identity.SimpleUser;
import com.stratocloud.job.JobDefinition;
import com.stratocloud.workflow.NodeInstanceStatus;
import com.stratocloud.workflow.Workflow;
import com.stratocloud.workflow.WorkflowInstanceStatus;
import com.stratocloud.workflow.nodes.ConfirmNode;
import com.stratocloud.workflow.nodes.EndNode;
import com.stratocloud.workflow.nodes.JobNode;
import com.stratocloud.workflow.nodes.StartNode;
import com.stratocloud.workflow.runtime.ConfirmNodeInstance;
import com.stratocloud.workflow.runtime.JobNodeInstance;
import com.stratocloud.workflow.runtime.WorkflowInstance;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.List;
import java.util.Map;
import java.util.Set;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestSequentialWorkflow {

    private Workflow workflow;

    @Before
    public void setUp(){
        StartNode startNode = new StartNode();
        ConfirmNode confirmNode = new ConfirmNode(
                List.of(
                        new SimpleUser(1L, "user1", "用户1"),
                        new SimpleUser(2L, "user2", "用户2")
                )
        );

        JobNode jobNode = new JobNode(
                new JobDefinition(
                        "TEST_JOB_TYPE",
                        "测试任务类型",
                        "TEST_JOB_START",
                        "TEST_JOB_CANCEL",
                        "test-service",
                        false
                )
        );
        EndNode endNode = new EndNode();

        startNode.connectTo(confirmNode);
        confirmNode.connectTo(jobNode);
        jobNode.connectTo(endNode);

        workflow = new Workflow("Test Workflow");
        workflow.addNodes(List.of(startNode, confirmNode, jobNode, endNode));
    }

    @Test
    public void testCase01(){
        WorkflowInstance workflowInstance = workflow.createInstance(Map.of());

        System.out.println(workflowInstance);

        workflowInstance.start();

        System.out.println(workflowInstance);

        var nodeInstances = workflowInstance.getNodeInstancesByStatus(Set.of(NodeInstanceStatus.STARTED));

        assert nodeInstances.size() == 1;

        assert nodeInstances.get(0) instanceof ConfirmNodeInstance;

        ConfirmNodeInstance confirmNodeInstance = (ConfirmNodeInstance) nodeInstances.get(0);

        assert confirmNodeInstance.getNode() instanceof ConfirmNode;

        ConfirmNode confirmNode = (ConfirmNode) confirmNodeInstance.getNode();

        assert confirmNode.getPossibleHandlers().get(0).loginName().equals("user1");

        workflowInstance.completeNode(confirmNodeInstance.getId());

        System.out.println(workflowInstance);

        nodeInstances = workflowInstance.getNodeInstancesByStatus(Set.of(NodeInstanceStatus.STARTED));

        assert nodeInstances.size() == 1;

        assert nodeInstances.get(0) instanceof JobNodeInstance;

        JobNodeInstance jobNodeInstance = (JobNodeInstance) nodeInstances.get(0);

        assert jobNodeInstance.getJob().getJobDefinition().getJobType().equals("TEST_JOB_TYPE");

        workflowInstance.completeNode(jobNodeInstance.getId());

        System.out.println(workflowInstance);

        assert workflowInstance.getStatus() == WorkflowInstanceStatus.FINISHED;
    }
}
