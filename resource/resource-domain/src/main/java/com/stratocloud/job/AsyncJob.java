package com.stratocloud.job;

import com.stratocloud.jpa.entities.Controllable;
import com.stratocloud.messaging.Message;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
@Getter
@Setter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AsyncJob extends Controllable {
    @Column(nullable = false)
    private String jobType;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column
    private Map<String, Object> runtimeVariables = new HashMap<>();
    @JdbcTypeCode(SqlTypes.JSON)
    @Column
    private Map<String, Object> outputVariables = new HashMap<>();
    @Column(nullable = false)
    private Boolean started;
    @Column(nullable = false)
    private Boolean ended;
    @BatchSize(size = 50)
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "asyncJob", orphanRemoval = true)
    private List<Execution> executions = new ArrayList<>();

    public AsyncJob(Long jobId, String jobType, Map<String, Object> runtimeVariables) {
        setId(jobId);
        this.jobType = jobType;

        this.started = false;
        this.ended = false;

        if(runtimeVariables!=null)
            this.runtimeVariables.putAll(runtimeVariables);
    }

    public void addExecution(Execution execution) {
        execution.setAsyncJob(this);
        executions.add(execution);
    }

    public void clearExecutions() {
        executions.clear();
    }

    public void checkTasksStates(){
        Stream<Execution> startedExecutions
                = executions.stream().filter(execution -> execution.getState() == ExecutionState.STARTED);

        startedExecutions.forEach(Execution::checkTasksStates);
    }

    public void discardCurrentExecutions() {
        for (Execution execution : executions) {
            execution.discard();
        }
        clearExecutions();
    }

    public void cancel(String message) {
        this.ended = true;
        for (Execution execution : executions) {
            execution.cancel(message);
        }
    }


    public void start() {
        if(this.started && !this.ended){
            log.warn("Async job {} has been already started and is not ended yet.", getId());
            return;
        }

        this.ended = false;

        if(executions.isEmpty()){
            checkExecutionsStates();
            return;
        }

        for (Execution execution : executions) {
            execution.start();
        }

        this.started = true;
    }



    public void checkExecutionsStates(){
        if(isAllExecutionsEnded()){
            this.ended = true;
            if(isAllExecutionsFinished()){
                Message message = Message.create(
                        JobTopics.WORKER_REPORT_JOB_FINISHED,
                        new WorkerReportJobFinishedPayload(getId(), outputVariables)
                );
                publish(message);
            }else {
                Message message = Message.create(
                        JobTopics.WORKER_REPORT_JOB_FAILED,
                        new WorkerReportJobFailedPayload(getId(), getErrorMessages())
                );
                publish(message);
            }
        }else {
            checkIfNotStartedSuccessfully();
        }
    }

    public void checkIfNotStartedSuccessfully(){
        if(isCreatedAnHourAgo() && isAnyExecutionAwaitingStart() && isNoExecutionRunning()) {
            this.ended = true;
            Message message = Message.create(
                    JobTopics.WORKER_REPORT_JOB_FAILED,
                    new WorkerReportJobFailedPayload(getId(), List.of("Job was not started successfully."))
            );
            publish(message);
        }
    }

    private boolean isCreatedAnHourAgo() {
        LocalDateTime createdAt = getCreatedAt();
        return createdAt!=null && createdAt.isBefore(LocalDateTime.now().minusHours(1L));
    }

    private boolean isAnyExecutionAwaitingStart() {
        return executions.stream().anyMatch(execution -> execution.getState() == ExecutionState.AWAIT_START);
    }

    private boolean isNoExecutionRunning(){
        return executions.stream().allMatch(execution -> execution.getState() != ExecutionState.STARTED);
    }


    private List<String> getErrorMessages(){
        List<String> result = new ArrayList<>();
        for (Execution execution : executions)
            if(execution.getState() == ExecutionState.FAILED)
                result.addAll(execution.getErrorMessages());
        return result;
    }

    private boolean isAllExecutionsFinished(){
        if(executions.isEmpty())
            return true;

        return executions.stream().allMatch(execution -> ExecutionState.FINISHED == execution.getState());
    }

    private boolean isAllExecutionsEnded(){
        if(executions.isEmpty())
            return true;

        return executions.stream().allMatch(execution -> ExecutionState.END_STATES.contains(execution.getState()));
    }

    public List<Task> getTasks(){
        List<Task> tasks = new ArrayList<>();
        for (Execution execution : executions)
            for (ExecutionStep step : execution.getSteps())
                tasks.addAll(step.getTasks());
        return tasks;
    }

    public JobHandler<?> getHandler(){
        return JobHandlerRegistry.getJobHandler(jobType);
    }


    public boolean isFailed(){
        return isAllExecutionsEnded() && !isAllExecutionsFinished();
    }


    public void addOutput(String key, Object value) {
        outputVariables.put(key, value);
    }
}
