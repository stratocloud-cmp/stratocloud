package com.stratocloud.job;

import com.stratocloud.workflow.Workflow;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class JobDefinition {
    @Id
    private String jobType;
    @Column(nullable = false)
    private String jobTypeName;
    @Column(nullable = false)
    private String startJobTopic;
    @Column(nullable = false)
    private String cancelJobTopic;
    @Column(nullable = false)
    private String serviceName;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private Workflow defaultWorkflow;

    @Column(nullable = false)
    private Boolean defaultWorkflowRequireOrder;

    public JobDefinition(String jobType,
                         String jobTypeName,
                         String startJobTopic,
                         String cancelJobTopic,
                         String serviceName, Boolean defaultWorkflowRequireOrder) {
        this.jobType = jobType;
        this.jobTypeName = jobTypeName;
        this.startJobTopic = startJobTopic;
        this.cancelJobTopic = cancelJobTopic;
        this.serviceName = serviceName;

        this.defaultWorkflow = Workflow.createSingleJobWorkflow(this);
        this.defaultWorkflowRequireOrder = defaultWorkflowRequireOrder;
    }
}
