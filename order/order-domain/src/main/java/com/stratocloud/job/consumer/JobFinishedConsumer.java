package com.stratocloud.job.consumer;

import com.stratocloud.job.Job;
import com.stratocloud.job.JobTopics;
import com.stratocloud.job.WorkerReportJobFinishedPayload;
import com.stratocloud.jpa.repository.EntityManager;
import com.stratocloud.messaging.Message;
import com.stratocloud.messaging.MessageConsumer;
import com.stratocloud.repository.WorkflowInstanceRepository;
import com.stratocloud.utils.JSON;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JobFinishedConsumer implements MessageConsumer {

    private final EntityManager entityManager;

    private final WorkflowInstanceRepository workflowInstanceRepository;

    public JobFinishedConsumer(EntityManager entityManager,
                               WorkflowInstanceRepository workflowInstanceRepository) {
        this.entityManager = entityManager;
        this.workflowInstanceRepository = workflowInstanceRepository;
    }

    @Override
    @Transactional
    public void consume(Message message) {
        WorkerReportJobFinishedPayload payload = JSON.toJavaObject(message.getPayload(), WorkerReportJobFinishedPayload.class);

        Job job = entityManager.findById(Job.class, payload.jobId());
        job.onFinished(payload.outputVariables());

        workflowInstanceRepository.save(job.getJobNodeInstance().getWorkflowInstance());
    }

    @Override
    public String getTopic() {
        return JobTopics.WORKER_REPORT_JOB_FINISHED;
    }

    @Override
    public String getConsumerGroup() {
        return "WORKER_REPORT_JOB_FINISHED_MASTER";
    }
}
