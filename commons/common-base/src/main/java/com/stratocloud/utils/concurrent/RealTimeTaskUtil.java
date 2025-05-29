package com.stratocloud.utils.concurrent;

import com.stratocloud.utils.SelfMonitorTarget;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

@Slf4j
@Component
public class RealTimeTaskUtil implements SelfMonitorTarget {
    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    public static <V> Future<V> submit(Callable<V> callable) {
        return executorService.submit(callable);
    }

    @Override
    public void logStats() {
        log.info("Real-time task executor service: {}", executorService);
    }

}
