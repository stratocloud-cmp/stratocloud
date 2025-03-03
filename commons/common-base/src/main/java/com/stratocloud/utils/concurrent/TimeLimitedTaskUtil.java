package com.stratocloud.utils.concurrent;

import com.stratocloud.utils.SelfMonitorTarget;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

@Slf4j
@Component
public class TimeLimitedTaskUtil implements SelfMonitorTarget {
    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    public static <T> T callWithTimeout(Callable<T> callable,
                                        long timeoutDuration,
                                        TimeUnit timeoutUnit) throws Exception {

        Future<T> future = executorService.submit(callable);

        try {
            return future.get(timeoutDuration, timeoutUnit);
        } catch (InterruptedException | TimeoutException e) {
            future.cancel(true);
            throw e;
        } catch (ExecutionException e) {
            throw throwCause(e);
        }
    }

    private static Exception throwCause(Exception e) throws Exception {
        Throwable cause = e.getCause();

        if (cause == null)
            throw e;

        if (cause instanceof Exception)
            throw (Exception) cause;

        if (cause instanceof Error)
            throw (Error) cause;

        throw e;
    }

    @Override
    public void logStats() {
        log.info("Time limited task executor service: {}", executorService);
    }

    public static ExecutorService getExecutorService() {
        return executorService;
    }
}
