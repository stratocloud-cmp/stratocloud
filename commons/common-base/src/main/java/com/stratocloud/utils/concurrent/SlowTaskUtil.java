package com.stratocloud.utils.concurrent;

import com.stratocloud.utils.SelfMonitorTarget;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

@Slf4j
@Component
public class SlowTaskUtil implements SelfMonitorTarget {
    private static final ExecutorService executorService = Executors.newFixedThreadPool(32);

    public static void submit(Runnable task){
        executorService.submit(task);
    }


    @Override
    public void logStats() {
        log.info("Slow task executor service: " + executorService);
    }

    public static ExecutorService getExecutorService() {
        return executorService;
    }
}
