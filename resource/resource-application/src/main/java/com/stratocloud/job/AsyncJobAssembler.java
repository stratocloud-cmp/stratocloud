package com.stratocloud.job;

import com.stratocloud.job.query.NestedAsyncJobResponse;
import com.stratocloud.job.query.NestedExecution;
import com.stratocloud.job.query.NestedExecutionStep;
import com.stratocloud.job.query.NestedTask;
import com.stratocloud.jpa.entities.EntityUtil;
import com.stratocloud.utils.JSON;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AsyncJobAssembler {
    public NestedAsyncJobResponse toNestedAsyncJobResponse(AsyncJob asyncJob) {
        NestedAsyncJobResponse nestedAsyncJobResponse = new NestedAsyncJobResponse();

        EntityUtil.copyBasicFields(asyncJob, nestedAsyncJobResponse);

        nestedAsyncJobResponse.setJobType(asyncJob.getJobType());
        nestedAsyncJobResponse.setEnded(asyncJob.getEnded());

        AsyncJobHandler<?> asyncJobHandler = AsyncJobService.getAsyncJobHandler(asyncJob);

        if(!asyncJobHandler.isTransientJob()){
            nestedAsyncJobResponse.setExecutions(toNestedExecutions(asyncJob.getExecutions()));
        }


        return nestedAsyncJobResponse;
    }

    private List<NestedExecution> toNestedExecutions(List<Execution> executions) {
        return executions.stream().map(this::toNestedExecution).toList();
    }

    private NestedExecution toNestedExecution(Execution execution) {
        NestedExecution nestedExecution = new NestedExecution();
        EntityUtil.copyBasicFields(execution, nestedExecution);
        nestedExecution.setState(execution.getState());
        nestedExecution.setSteps(toNestedSteps(execution.getSteps()));
        return nestedExecution;
    }

    private List<NestedExecutionStep> toNestedSteps(List<ExecutionStep> steps) {
        return steps.stream().map(this::toNestedStep).toList();
    }

    private NestedExecutionStep toNestedStep(ExecutionStep step) {
        NestedExecutionStep nestedExecutionStep = new NestedExecutionStep();
        EntityUtil.copyBasicFields(step, nestedExecutionStep);
        nestedExecutionStep.setStepIndex(step.getStepIndex());
        nestedExecutionStep.setState(step.getState());
        nestedExecutionStep.setTasks(toNestedTasks(step.getTasks()));
        return nestedExecutionStep;
    }

    private List<NestedTask> toNestedTasks(List<Task> tasks) {
        return tasks.stream().map(this::toNestedTask).toList();
    }

    private NestedTask toNestedTask(Task task) {
        NestedTask nestedTask = new NestedTask();

        EntityUtil.copyBasicFields(task, nestedTask);

        nestedTask.setName(task.getName());
        nestedTask.setState(task.getState());
        nestedTask.setEntityClass(task.getEntityClass().getSimpleName());
        nestedTask.setEntityId(task.getEntityId());
        nestedTask.setEntityDescription(task.getEntityDescription());
        nestedTask.setType(task.getType());
        nestedTask.setTypeName(task.getTypeName());
        nestedTask.setTaskInputs(JSON.toMap(task.getTaskInputs()));
        nestedTask.setTaskInputsClass(task.getTaskInputsClass().getSimpleName());
        nestedTask.setMessage(task.getMessage());

        return nestedTask;
    }
}
