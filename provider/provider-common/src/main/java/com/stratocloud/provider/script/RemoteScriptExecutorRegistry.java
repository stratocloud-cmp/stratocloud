package com.stratocloud.provider.script;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class RemoteScriptExecutorRegistry {

    private static final Map<RemoteScriptExecutor.ExecutionType, RemoteScriptExecutor> map = new ConcurrentHashMap<>();

    public static void register(RemoteScriptExecutor executor) {
        map.put(executor.getExecutionType(), executor);
    }

    public static Optional<RemoteScriptExecutor> getExecutor(RemoteScriptExecutor.ExecutionType executionType){
        return Optional.ofNullable(map.get(executionType));
    }
}
