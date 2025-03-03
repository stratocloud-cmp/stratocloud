package com.stratocloud.workflow.factory;

import com.stratocloud.job.JobDefinition;
import com.stratocloud.jpa.repository.EntityManager;
import com.stratocloud.utils.Assert;
import com.stratocloud.workflow.Node;
import com.stratocloud.workflow.NodeFactory;
import com.stratocloud.workflow.nodes.JobNode;
import org.springframework.stereotype.Component;

@Component
public class JobNodeFactory implements NodeFactory<JobNodeProperties> {

    private final EntityManager entityManager;

    public JobNodeFactory(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public String getNodeType() {
        return "JOB_NODE";
    }

    @Override
    public String getNodeTypeName() {
        return "任务节点";
    }

    @Override
    public Node createNode(String nodeKey, String nodeName, JobNodeProperties nodeProperties) {
        Assert.isNotBlank(nodeProperties.getJobType(), "节点[%s]未选择任务类型".formatted(nodeName));

        JobDefinition jobDefinition = entityManager.findById(JobDefinition.class, nodeProperties.getJobType());
        JobNode jobNode = new JobNode(jobDefinition);
        jobNode.setNodeKey(nodeKey);
        jobNode.setNodeType(getNodeType());
        jobNode.setName(nodeName);
        return jobNode;
    }
}
