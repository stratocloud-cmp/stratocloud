package com.stratocloud.job;


import com.stratocloud.jpa.entities.Controllable;
import com.stratocloud.messaging.Message;
import com.stratocloud.workflow.runtime.JobNodeInstance;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Getter
@Setter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Job extends Controllable {
    @Column(nullable = false)
    private Boolean manualStart;
    @Column
    private LocalDateTime plannedStartTime;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private JobStatus status;
    @Column
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> parameters;
    @Column
    private LocalDateTime startedAt;
    @Column
    private LocalDateTime endedAt;
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    @ManyToOne
    private JobDefinition jobDefinition;
    @OneToOne
    private JobNodeInstance jobNodeInstance;



    public Job(JobDefinition jobDefinition, JobNodeInstance jobNodeInstance) {
        this.jobDefinition = jobDefinition;
        this.jobNodeInstance = jobNodeInstance;

        this.manualStart = false;
        this.status = JobStatus.AWAIT_START;
    }


    public void start(Map<String, Object> runtimeVariables){
        onEvent(JobEvent.START_REQUESTED);
        this.startedAt = LocalDateTime.now();

        Message startJobMessage = Message.create(
                jobDefinition.getStartJobTopic(),
                new StartJobPayload(getId(), parameters, runtimeVariables)
        );
        publish(startJobMessage);
    }

    public void retry(){
        onEvent(JobEvent.RETRY_REQUESTED);
        jobNodeInstance.onJobRestarted();
        Message startJobMessage = Message.create(
                jobDefinition.getStartJobTopic(),
                new StartJobPayload(
                        getId(),
                        parameters,
                        jobNodeInstance.getWorkflowInstance().getRuntimeProperties()
                )
        );
        publish(startJobMessage);
    }

    public void cancel(String message){
        onEvent(JobEvent.CANCEL_REQUESTED);
        this.errorMessage = message;

        Message cancelJobMessage = Message.create(
                jobDefinition.getCancelJobTopic(),
                new JobCanceledPayload(getId(), message, parameters)
        );
        publish(cancelJobMessage);
    }

    public void onFinished(Map<String, Object> outputVariables){
        onEvent(JobEvent.WORKER_REPORT_FINISHED);
        this.endedAt = LocalDateTime.now();

        if(jobNodeInstance != null){
            jobNodeInstance.getWorkflowInstance().addRuntimeProperties(outputVariables);
            jobNodeInstance.complete();
        }
    }

    public void onFailure(String errorMessage){
        onEvent(JobEvent.WORKER_REPORT_FAILURE);
        this.errorMessage = errorMessage;
        this.endedAt = LocalDateTime.now();

        if(jobNodeInstance != null){
            jobNodeInstance.onFailed(errorMessage);
        }
    }

    private void onEvent(JobEvent event){
        JobStatus nextState = JobStateMachine.get().getNextState(status, event);

        if(nextState == null)
            return;

        status = nextState;
    }

    public void reassignId(Long jobId){
        setId(jobId);
    }
}
