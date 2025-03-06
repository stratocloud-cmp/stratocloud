package com.stratocloud.job;

import com.stratocloud.exceptions.StratoException;
import com.stratocloud.jpa.entities.Auditable;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.util.*;

@SuppressWarnings("ALL")
@Getter
@Setter
@Entity
public class Execution extends Auditable {
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ExecutionState state = ExecutionState.AWAIT_START;
    @ManyToOne(fetch = FetchType.LAZY)
    private AsyncJob asyncJob;
    @BatchSize(size = 50)
    @Getter(AccessLevel.NONE)
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "execution", orphanRemoval = true)
    private List<ExecutionStep> steps = new ArrayList<>();

    public static Execution of(Task task){
        Execution execution = new Execution();
        ExecutionStep step = new ExecutionStep();
        step.addTask(task);
        execution.addStep(step);
        return execution;
    }

    public void checkTasksStates(){
        List<ExecutionStep> startedSteps
                = steps.stream().filter(step -> step.getState() == ExecutionStepState.STARTED).toList();

        startedSteps.forEach(ExecutionStep::checkTasksStates);
    }

    public void addStep(ExecutionStep step){
        if(step.isEmpty())
            return;

        step.setStepIndex(steps.size());
        step.setExecution(this);
        steps.add(step);
    }

    public void insertStep(int index, ExecutionStep step){
        if(step.isEmpty())
            return;

        step.setExecution(this);
        steps.add(index, step);
        adjustStepsIndex();
    }

    private void adjustStepsIndex() {
        for (int i = 0; i < steps.size(); i++) {
            steps.get(i).setStepIndex(i);
        }
    }

    public List<ExecutionStep> getSteps() {
        return steps.stream().sorted(Comparator.comparingInt(ExecutionStep::getStepIndex)).toList();
    }

    public void onStepFinished(ExecutionStep step) {
        Optional<ExecutionStep> nextStep = getNextStep(step);

        if(nextStep.isPresent()) {
            nextStep.get().start();
        } else {
            this.state = ExecutionState.FINISHED;
            asyncJob.checkExecutionsStates();
        }
    }

    private List<ExecutionStep> getSequencedSteps(){
        return steps.stream().sorted(Comparator.comparingInt(ExecutionStep::getStepIndex)).toList();
    }

    private Optional<ExecutionStep> getNextStep(ExecutionStep step){
        List<ExecutionStep> sequencedSteps = getSequencedSteps();
        for (int i = 0; i < sequencedSteps.size()-1; i++) {
            if(step.getId().equals(sequencedSteps.get(i).getId())){
                return Optional.of(sequencedSteps.get(i+1));
            }
        }
        return Optional.empty();
    }

    private Optional<ExecutionStep> getFirstStep(){
        List<ExecutionStep> sequencedSteps = getSequencedSteps();
        if(sequencedSteps.size() == 0)
            return Optional.empty();
        else
            return Optional.of(sequencedSteps.get(0));
    }

    public void onStepFailed(ExecutionStep step) {
        this.state = ExecutionState.FAILED;
        asyncJob.checkExecutionsStates();
    }

    public void discard() {
        if(state != ExecutionState.AWAIT_START)
            throw new StratoException("Cannot discard execution from %s state.".formatted(state));

        state = ExecutionState.DISCARDED;
        steps.forEach(ExecutionStep::discard);
    }

    public void cancel(String message) {
        state = ExecutionState.CANCELED;
        steps.forEach(step -> step.cancel(message));
    }

    public void start() {
        if(state == ExecutionState.FINISHED){
            asyncJob.checkExecutionsStates();
            return;
        }

        if(state != ExecutionState.AWAIT_START && state != ExecutionState.FAILED)
            throw new StratoException("Cannot start execution from %s state.".formatted(state));

        Optional<ExecutionStep> firstStep = getFirstStep();

        if(firstStep.isPresent()) {
            this.state = ExecutionState.STARTED;
            firstStep.get().start();
        }else {
            this.state = ExecutionState.FINISHED;
            asyncJob.checkExecutionsStates();
        }
    }

    public List<String> getErrorMessages() {
        List<String> result = new ArrayList<>();

        for (ExecutionStep step : getSequencedSteps())
            if(step.getState() == ExecutionStepState.FAILED)
                result.addAll(step.getErrorMessages());


        return result;
    }
}
