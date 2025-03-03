package com.stratocloud.job;

public interface TaskHandler {
    TaskType getTaskType();

    String getTaskName(Long entityId, TaskInputs taskInputs);
    void start(Task task);
    void checkResult(Task task);
    void onDiscard(Task task);

    default void postHandleTaskFailure(Task task){}

    default boolean isIdempotent(){return false;}
}
