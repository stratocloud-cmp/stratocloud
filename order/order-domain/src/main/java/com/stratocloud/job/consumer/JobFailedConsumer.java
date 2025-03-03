package com.stratocloud.job.consumer;

import com.stratocloud.exceptions.EntityNotFoundException;
import com.stratocloud.job.Job;
import com.stratocloud.job.JobTopics;
import com.stratocloud.job.WorkerReportJobFailedPayload;
import com.stratocloud.messaging.Message;
import com.stratocloud.messaging.MessageConsumer;
import com.stratocloud.repository.JobRepository;
import com.stratocloud.repository.WorkflowInstanceRepository;
import com.stratocloud.utils.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class JobFailedConsumer implements MessageConsumer {

    private final JobRepository jobRepository;

    private final WorkflowInstanceRepository workflowInstanceRepository;

    private static final int MAX_ERROR_MESSAGES_SIZE = 20;

    public JobFailedConsumer(JobRepository jobRepository,
                             WorkflowInstanceRepository workflowInstanceRepository) {
        this.jobRepository = jobRepository;
        this.workflowInstanceRepository = workflowInstanceRepository;
    }

    @Override
    @Transactional
    public void consume(Message message) {
        WorkerReportJobFailedPayload payload = JSON.toJavaObject(message.getPayload(), WorkerReportJobFailedPayload.class);

        Job job = jobRepository.findById(payload.jobId()).orElseThrow(
                () -> new EntityNotFoundException("Job not found by id: %s.".formatted(payload.jobId()))
        );

        List<String> errorMessages = new ArrayList<>();
        if(payload.errorMessages() != null){
            if(payload.errorMessages().size() > MAX_ERROR_MESSAGES_SIZE){
                log.warn("Too many error messages of job {}. Cutting down to {}.", job.getId(), MAX_ERROR_MESSAGES_SIZE);
                errorMessages.addAll(payload.errorMessages().subList(0, MAX_ERROR_MESSAGES_SIZE));
                errorMessages.add("...");
            }else {
                errorMessages.addAll(payload.errorMessages());
            }
        }
        String joinedMessage = String.join("\n", errorMessages);
        job.onFailure(joinedMessage);
        workflowInstanceRepository.save(job.getJobNodeInstance().getWorkflowInstance());
    }

    @Override
    public String getTopic() {
        return JobTopics.WORKER_REPORT_JOB_FAILED;
    }

    @Override
    public String getConsumerGroup() {
        return "WORKER_REPORT_JOB_FAILED_MASTER";
    }
}
