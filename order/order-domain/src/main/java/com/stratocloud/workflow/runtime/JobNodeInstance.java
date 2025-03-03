package com.stratocloud.workflow.runtime;

import com.stratocloud.job.Job;
import com.stratocloud.messaging.Message;
import com.stratocloud.workflow.NodeInstanceEvent;
import com.stratocloud.workflow.messaging.WorkflowReportJobFailedPayload;
import com.stratocloud.workflow.messaging.WorkflowReportJobFinishedPayload;
import com.stratocloud.workflow.messaging.WorkflowReportJobStartedPayload;
import com.stratocloud.workflow.messaging.WorkflowTopics;
import com.stratocloud.workflow.nodes.JobNode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobNodeInstance extends NodeInstance {

    @OneToOne(mappedBy = "jobNodeInstance", cascade = CascadeType.ALL)
    private Job job;

    public JobNodeInstance(JobNode jobNode) {
        super(jobNode);
        this.job = new Job(jobNode.getJobDefinition(), this);
    }

    public JobNode getJobNode(){
        return (JobNode) super.getNode();
    }

    @Override
    protected void start() {
        super.start();

        job.start(workflowInstance.getRuntimeProperties());

        Message message = Message.create(
                WorkflowTopics.WORKFLOW_REPORT_JOB_STARTED,
                new WorkflowReportJobStartedPayload(getId(), job.getId()),
                workflowInstance.getId().toString()
        );

        publish(message);
    }

    public void onJobRestarted() {
        onEvent(NodeInstanceEvent.RESTART_REQUESTED);
        workflowInstance.onNodeRestarted();

        Message message = Message.create(
                WorkflowTopics.WORKFLOW_REPORT_JOB_STARTED,
                new WorkflowReportJobStartedPayload(getId(), job.getId()),
                workflowInstance.getId().toString()
        );

        publish(message);
    }

    @Override
    protected void cancel(String message) {
        super.cancel(message);

        job.cancel(message);
    }

    @Override
    protected void discard() {
        super.discard();

        job.cancel(null);
    }

    @Override
    public void complete() {
        Message message = Message.create(
                WorkflowTopics.WORKFLOW_REPORT_JOB_FINISHED,
                new WorkflowReportJobFinishedPayload(getId(), job.getId()),
                workflowInstance.getId().toString()
        );

        publish(message);

        super.complete();
    }

    @Override
    public void onFailed(String errorMessage) {
        Message message = Message.create(
                WorkflowTopics.WORKFLOW_REPORT_JOB_FAILED,
                new WorkflowReportJobFailedPayload(getId(), job.getId()),
                workflowInstance.getId().toString()
        );

        publish(message);

        super.onFailed(errorMessage);
    }
}
