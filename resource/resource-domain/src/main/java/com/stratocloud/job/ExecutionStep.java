package com.stratocloud.job;

import com.stratocloud.exceptions.StratoException;
import com.stratocloud.jpa.entities.Auditable;
import com.stratocloud.utils.concurrent.ConcurrentUtil;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
public class ExecutionStep extends Auditable {
    @Column(nullable = false)
    private Integer stepIndex;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ExecutionStepState state = ExecutionStepState.AWAIT_START;
    @ManyToOne(fetch = FetchType.LAZY)
    private Execution execution;
    @BatchSize(size = 50)
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "step", orphanRemoval = true)
    private List<Task> tasks = new ArrayList<>();

    public static ExecutionStep of(Task... tasks){
        ExecutionStep executionStep = new ExecutionStep();
        if(tasks != null)
            for (Task task : tasks)
                executionStep.addTask(task);

        return executionStep;
    }

    public void addTask(Task task){
        task.setStep(this);
        this.tasks.add(task);
    }


    public void checkTasksStates(){
        if(isAllTasksEnded()){
            if(isAllTasksFinished()){
                this.state = ExecutionStepState.FINISHED;
                execution.onStepFinished(this);
            }else {
                this.state = ExecutionStepState.FAILED;
                execution.onStepFailed(this);
            }
        }

        startAwaitStartTasks();
    }

    private void startAwaitStartTasks() {
        List<Task> awaitStartTasks = tasks.stream().filter(
                t -> t.getState() == TaskState.AWAIT_START
        ).toList();

        List<Runnable> runnableList = awaitStartTasks.stream().map(t -> (Runnable) t::start).toList();

        ConcurrentUtil.runAndWait(runnableList);
    }

    private boolean isAllTasksFinished(){
        return tasks.stream().allMatch(task -> TaskState.FINISHED == task.getState());
    }

    private boolean isAllTasksEnded(){
        return tasks.stream().allMatch(task -> TaskState.END_STATES.contains(task.getState()));
    }

    public void start() {
        if(state == ExecutionStepState.FINISHED) {
            checkTasksStates();
            return;
        }

        if(state != ExecutionStepState.AWAIT_START && state != ExecutionStepState.FAILED)
            throw new StratoException("Cannot start execution step from %s state.".formatted(state));

        List<Runnable> runnableList = tasks.stream().map(task -> (Runnable) task::start).toList();

        ConcurrentUtil.runAndWait(runnableList);

        this.state = ExecutionStepState.STARTED;
    }

    public List<String> getErrorMessages() {
        return tasks.stream().filter(task -> task.getState()==TaskState.FAILED).map(Task::getMessage).toList();
    }

    public void discard() {
        if(state != ExecutionStepState.AWAIT_START)
            throw new StratoException("Cannot discard execution step from %s state.".formatted(state));

        tasks.forEach(Task::discard);
        state = ExecutionStepState.DISCARDED;
    }

    public void cancel(String message) {
        tasks.forEach(task -> task.cancel(message));
        this.state = ExecutionStepState.CANCELED;
    }

    public boolean isEmpty() {
        return tasks.isEmpty();
    }
}
