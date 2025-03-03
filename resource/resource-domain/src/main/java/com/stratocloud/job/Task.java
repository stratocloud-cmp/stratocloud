package com.stratocloud.job;

import com.stratocloud.exceptions.AutoRetryLaterException;
import com.stratocloud.fsm.StateMachine;
import com.stratocloud.jpa.entities.Auditable;
import com.stratocloud.utils.JSON;
import com.stratocloud.utils.Utils;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.Map;

@Slf4j
@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Task extends Auditable {
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TaskState state;
    @Column(nullable = false)
    private Class<? extends TaskTargetEntity> entityClass;
    @Column(nullable = false)
    private Long entityId;
    @Column(nullable = false)
    private String entityDescription;
    @Column(nullable = false)
    private String type;
    @Column(nullable = false)
    private String typeName;
    @Column
    @JdbcTypeCode(SqlTypes.JSON)
    @Getter(AccessLevel.NONE)
    private Map<String, Object> taskInputs;
    @Column(nullable = false)
    private Class<? extends TaskInputs> taskInputsClass;
    @Column(columnDefinition = "TEXT")
    private String message;
    @Column(nullable = false)
    private Integer retriedTimes = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    private ExecutionStep step;

    @Column
    private String externalTaskId;

    public Task(TaskTargetEntity entity, TaskType taskType, TaskInputs taskInputs){
        this.entityClass = entity.getClass();
        this.entityId = entity.getId();
        this.entityDescription = entity.getEntityDescription();

        this.type = taskType.type();
        this.typeName = taskType.name();

        this.taskInputs = JSON.toMap(taskInputs);
        this.taskInputsClass = taskInputs.getClass();

        this.state = TaskState.AWAIT_START;
        this.name = TaskHandlerRegistry.getTaskHandler(taskType.type()).getTaskName(entity.getId(), taskInputs);
    }

    public TaskInputs getTaskInputs(){
        return JSON.convert(taskInputs, taskInputsClass);
    }

    private void onEvent(TaskEvent event){
        StateMachine<TaskState, TaskEvent> stateMachine = TaskStateMachine.get();
        TaskState nextState = stateMachine.getNextState(state, event);

        if(nextState == null)
            return;

        state = nextState;
    }

    public void start(){
        if(state == TaskState.FINISHED)
            return;

        if(state == TaskState.FAILED){
            retriedTimes++;
            onEvent(TaskEvent.RETRY_REQUESTED);
        }else {
            onEvent(TaskEvent.START_REQUESTED);
        }

        if(retriedTimes >= 30){
            log.error("Max retry limit of task {} exceeded.", getId());
            onFailed("Max retry limit exceeded: " + message);
            return;
        }

        if(retriedTimes > 0){
            log.warn("Retrying task: {}.", getId());
        }

        try {
            TaskContext.create(this);
            TaskHandlerRegistry.getTaskHandler(type).start(this);
        } catch (AutoRetryLaterException e){
            handleRetryLater(e);
        }catch (OptimisticLockingFailureException e) {
            if(TaskHandlerRegistry.getTaskHandler(type).isIdempotent()){
                handleRetryLater(e);
            }else {
                log.error("Failed to start task {}.", getId(), e);
                onFailed("系统繁忙，请稍后重试。");
            }
        }catch (Exception e){
            log.error("Failed to start task {}.", getId(), e);
            if(Utils.isBlank(e.getMessage()))
                onFailed("Failed to start task.");
            else
                onFailed("Failed to start task: %s".formatted(e.getMessage()));
        }finally {
            TaskContext.remove();
        }
    }

    private void handleRetryLater(Exception e) {
        onEvent(TaskEvent.AUTO_RETRY_START_LATER);
        log.warn("Starting task {} later: {}", getId(), e.getMessage());
        message = e.getMessage();
        retriedTimes++;
    }

    public void checkResult(){
        try {
            TaskContext.create(this);
            TaskHandlerRegistry.getTaskHandler(type).checkResult(this);
        }catch (AutoRetryLaterException | OptimisticLockingFailureException e){
            log.warn("Checking task {} result later: {}", getId(), e.getMessage());
        }catch (Exception e){
            log.error("Failed to check task {} result.", getId(), e);
            onFailed("Failed to check task result: "+e.getMessage());
        }finally {
            TaskContext.remove();
        }
    }

    public void discard() {
        onEvent(TaskEvent.DISCARD_REQUESTED);
        TaskContext.create(this);
        TaskHandlerRegistry.getTaskHandler(type).onDiscard(this);
        TaskContext.remove();
    }

    public void onFinished(){
        onEvent(TaskEvent.HANDLER_REPORT_FINISHED);

        log.info("{} task on [{}] succeeded.", type, entityDescription);
    }

    public void onFailed(String message){
        onEvent(TaskEvent.HANDLER_REPORT_FAILED);
        this.message = message;
        log.error("{} task on [{}] failed: {}.", type, entityDescription, message);
        try {
            log.warn("Post handling task failure...TaskId={}.", getId());
            TaskHandlerRegistry.getTaskHandler(type).postHandleTaskFailure(this);
        } catch (Exception e){
            log.error("Failed to post handle task failure. TaskId={}.", getId(), e);
        }

    }

    public void cancel(String message) {
        if(state != TaskState.AWAIT_START)
            return;

        onEvent(TaskEvent.CANCEL_REQUESTED);
        this.message = message;

        TaskContext.create(this);
        TaskHandlerRegistry.getTaskHandler(type).onDiscard(this);
        TaskContext.remove();
    }
}
