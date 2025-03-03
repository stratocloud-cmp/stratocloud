package com.stratocloud.workflow.nodes;

import com.stratocloud.job.JobDefinition;
import com.stratocloud.utils.Assert;
import com.stratocloud.workflow.Node;
import com.stratocloud.workflow.NodeProperties;
import com.stratocloud.workflow.factory.JobNodeProperties;
import com.stratocloud.workflow.runtime.JobNodeInstance;
import com.stratocloud.workflow.runtime.NodeInstance;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobNode extends Node {
    @ManyToOne
    private JobDefinition jobDefinition;

    public JobNode(JobDefinition jobDefinition) {
        this.jobDefinition = jobDefinition;
    }

    @Override
    public NodeInstance createInstance() {
        return new JobNodeInstance(this);
    }

    @Override
    public NodeProperties getProperties() {
        JobNodeProperties properties = new JobNodeProperties();
        properties.setJobType(jobDefinition.getJobType());
        return properties;
    }

    @Override
    public void validate() {
        super.validate();
        Assert.isNotNull(jobDefinition, "节点[%s]未选择任务类型".formatted(getName()));
    }
}
