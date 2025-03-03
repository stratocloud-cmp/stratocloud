package com.stratocloud.job;

import com.stratocloud.utils.Utils;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class TaskContext {

    private final Task task;
    private static final ThreadLocal<TaskContext> threadLocal = new ThreadLocal<>();

    public TaskContext(Task task) {
        this.task = task;
    }

    private static Optional<TaskContext> getContext(){
        return Optional.ofNullable(threadLocal.get());
    }

    private static Optional<Task> getTask(){
        return getContext().map(c -> c.task);
    }

    public static void setExternalTaskId(String externalTaskId){
        getTask().ifPresentOrElse(
                t -> t.setExternalTaskId(externalTaskId),
                () -> log.warn("External task {} running without context.", externalTaskId)
        );
    }

    public static Optional<String> getExternalTaskId(){
        return getTask().map(Task::getExternalTaskId).filter(Utils::isNotBlank);
    }

    public static void create(Task task){
        threadLocal.set(new TaskContext(task));
    }

    public static void remove(){
        threadLocal.remove();
    }
}
