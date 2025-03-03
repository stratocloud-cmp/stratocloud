package com.stratocloud.job;

import com.stratocloud.exceptions.StratoException;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class TaskHandlerRegistry {
    private static final Map<String, TaskHandler> taskHandlerMap = new ConcurrentHashMap<>();

    public static void register(TaskHandler taskHandler){
        String type = taskHandler.getTaskType().type();
        if(taskHandlerMap.containsKey(type))
            throw new StratoException("Cannot register 2 task handlers with same task type: %s".formatted(type));

        taskHandlerMap.put(type, taskHandler);
        log.info("Task handler {} registered.", taskHandler.getTaskType().type());
    }

    public static TaskHandler getTaskHandler(String type){
        TaskHandler taskHandler = taskHandlerMap.get(type);
        if(taskHandler == null)
            throw new StratoException("Task handler of type %s does not exist.".formatted(type));

        return taskHandler;
    }
}
